(ns zou.framework.core
  (:require [clojure.java.io :as io]
            [zou.component :as c]
            [zou.framework.config :as conf]
            [zou.framework.ext :as ext]
            [zou.logging :as log]
            [zou.specter :as s]
            [zou.util :as u]
            [zou.util.namespace :as ns]
            [zou.util.repl :as ur]))

(def ^:private bootstrap-config-path "zou/config/bootstrap.edn")
(def ^:private main-system-key :main)

(defonce ^:private core-system nil)

(defn core []
  (:core core-system))

(defn read-bootstrap-config []
  (conf/read-config (io/resource bootstrap-config-path)))

(declare systems)

(defrecord Core []
  c/Lifecycle
  (start [this]
    (assoc this :systems (atom {})))
  (stop [this]
    (doseq [[k s] (systems this)]
      (log/infof "Stopping a system: %s" k)
      (c/stop s))
    this))

(def systems-path
  (s/comp-paths :systems some? s/atom-path))

(def system-path
  (s/comp-paths systems-path s/keypath))

(defn system
  ([core]
   (system core main-system-key))
  ([core system-key]
   (s/select-one (system-path system-key) core)))

(defn systems
  ([]
   (systems (core)))
  ([core]
   (s/select-one systems-path core)))

(defn add-system [core system-key system]
  (s/setval (system-path system-key) system core))

(defn add-systems [core systems]
  (reduce-kv add-system core systems))

(defn start-system [core system-key]
  (s/transform (system-path system-key) #(c/try-recovery (c/start %)) core))

(defn start-systems [core]
  (doseq [k (keys (systems core))]
    (start-system core k)))

(defn stop-system [core system-key]
  (s/transform (system-path system-key) c/stop core))

(defn stop-systems [core]
  (doseq [k (keys (systems core))]
    (stop-system core k)))

(defn remove-system [core system-key]
  (s/transform systems-path #(dissoc % system-key) core))

(defn load-system
  ([core conf-map-or-source]
   (load-system core conf-map-or-source main-system-key))
  ([core conf-map-or-source system-key]
   (->> conf-map-or-source
        (u/?>> (not (map? conf-map-or-source)) (conf/read-config))
        c/build-nested-system-map
        (add-system core system-key))))

(defn make-core-from-conf [conf]
  (c/build-system-map (read-bootstrap-config)))

(defn boot-core!
  ([] (boot-core! (read-bootstrap-config)))
  ([conf] (boot-core! #'core-system conf))
  ([core-var conf]
   (alter-var-root
    core-var
    (fn [old]
      (if (nil? old)
        (c/try-recovery (c/start (make-core-from-conf conf)))
        (do
          (log/warn "Core System is already running. Rebooting...")
          (c/stop old)
          (c/start (make-core-from-conf (read-bootstrap-config)))))))))

(defn shutdown-core!
  ([] (shutdown-core! #'core-system))
  ([core-var]
   (alter-var-root
    core-var
    (fn [sys]
      (when sys
        (c/stop sys))
      nil))))


;;; Built-in core modules
;;; ---------------------

(defrecord AutoLoader [exclude-classpath prefixes]
  c/Lifecycle
  (start [this]
    (when (seq prefixes)
      (apply ns/require-all exclude-classpath prefixes))
    this)
  (stop [this]
    this))

(defrecord LoggingConfigurator [bridge-out?]
  c/Lifecycle
  (start [this]
    (when bridge-out?
      (ur/bridge-out!))
    (log/start-logging! (dissoc (into {} this)
                                :bridge-out?))
    this)
  (stop [this]
    (ur/restore-out!)
    this))

(defrecord ExtensionLoader []
  c/Lifecycle
  (start [this]
    (ext/initialize-extensions)
    this)
  (stop [this]
    (ext/deinitialize-extensions)
    this))
