(ns zou.framework.core
  (:require [schema.core :as s]
            [zou.component :as c]
            [zou.framework.config :as conf]
            [zou.framework.entrypoint.proto :as ep]
            [zou.logging :as log]))

(defonce ^:private +core-system+ nil)

(s/defrecord CoreSystem [entrypoint :- (s/protocol ep/EntryPoint)]
  {s/Keyword s/Any}
  c/Lifecycle
  (start [this]
    (s/validate CoreSystem this)
    (c/start-system this))
  (stop [this]
    (c/stop-system this))
  ep/EntryPoint
  (run [this args]
    (ep/run entrypoint args)))

(defn make-core
  "Create an instance of CoreSystem with the given config map."
  [conf]
  (map->CoreSystem (c/build-system-map conf)))

(defn boot-core!
  "Create an instance of CoreSystem with the given config map, and
  start its life cycle. If the config map is not specified, it will be
  fetched via `zou.framework.config/fetch-config-or-abort`.

  Also, the started instance will be bound to the given var. If the
  var is not specified, `#'zou.framework.core/+core-system+` will be
  used. Note that an old instance bound to the given var will be
  stopped before creating a new instance if exists."
  ([] (boot-core! (conf/read-config (conf/fetch-config-or-abort))))
  ([conf] (boot-core! #'+core-system+ conf))
  ([core-var conf]
   (alter-var-root
    core-var
    (fn [old]
      (let [boot #(c/try-recovery (c/start (make-core conf)))]
        (if (nil? old)
          (boot)
          (do
            (log/warn "Core System is already running. Rebooting...")
            (c/stop old)
            (boot))))))))

(defn shutdown-core!
  "Stop the core system bound to the given var. If the var is not
  specified, `#'zou.framework.core/+core-system+` will be used."
  ([] (shutdown-core! #'+core-system+))
  ([core-var]
   (alter-var-root
    core-var
    (fn [sys]
      (when sys
        (c/stop sys))
      nil))))

(defn run-core
  "Run the given core system with `zou.framework.entrypoint.proto/EntryPoint#run`.
  The core system will delegate its process to an entry point
  component that is a member of it. By default,
  `zou.framework.entrypoint.impl.DefaultEntryPoint` is assinged to the
  etnry point component."
  [core-system & args]
  (ep/run core-system args))

(defn core-system
  "Return the current instance of CoreSystem. You should use this only
  for debugging purposes or for convenience during development. Note
  that this works only when the instance is bound to
  `#'zou.framework.core/+core-system+`."
  []
  (when-not (bound? #'+core-system+)
    (throw (RuntimeException. "Failed to find a core system. Did you forget to start it?")))
  +core-system+)
