(ns zou.cljs.figwheel
  (:require [figwheel-sidecar.config :as conf]
            [figwheel-sidecar.system :as sys]
            [zou.component :as c]
            [zou.util.namespace :as un]
            [zou.web.asset.proto :as aproto]))

(un/import-vars [figwheel-sidecar.components.css-watcher css-watcher])
(un/import-vars [figwheel-sidecar.system cljs-repl])

(defn ->figwheel-config-source [conf]
  (reify conf/ConfigSource
    (-config-data [_]
      (conf/map->FigwheelConfigData
       {:data
        ;; XXX: relax the validation to allow our extra option keys (e.g. :serve-path).
        (assoc conf :validate-config :ignore-unknown-keys)}))))

(defn ->config-data [conf]
  (conf/config-source->prepped-figwheel-internal (->figwheel-config-source (into {} conf))))

(defrecord Figwheel [builds
                     ;; & ks <-- any other figwheel options are acceptable
                     ]
  c/Lifecycle
  (start [this]
    (assoc this
           :figwheel-system
           (-> (->config-data this)
               sys/create-figwheel-system
               c/start)))
  (stop [this]
    (when-let [fs (:figwheel-system this)]
      (c/stop fs))
    (assoc this :figwheel-system nil))

  aproto/AssetsProvider
  (assets [this]
    (map (fn [v]
           {:name (:serve-path v)
            :type :javascript
            :src  (or (get-in v [:build-options :output-to])
                      (get-in v [:compiler :output-to]))})
         (conf/all-builds (->config-data this)))))

(defn figwheel [conf]
  (map->Figwheel conf))
