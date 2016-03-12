(ns zou.cljs.figwheel
  (:require [figwheel-sidecar.config :as conf]
            [figwheel-sidecar.system :as sys]
            [zou.component :as c]
            [zou.util.namespace :as un]
            [zou.web.asset.proto :as aproto]))

(un/import-vars [figwheel-sidecar.components.css-watcher css-watcher])
(un/import-vars [figwheel-sidecar.system cljs-repl])

(defn- extract-build-conf [builds]
  (:all-builds (conf/prep-config (conf/config {:cljsbuild {:builds builds}}))))

(defrecord Figwheel [figwheel-options all-builds build-ids builds]
  c/Lifecycle
  (start [this]
    (assoc this
           :figwheel-system
           (-> (conf/prep-config {:figwheel-options figwheel-options
                                  :all-builds (concat all-builds
                                                      (extract-build-conf builds))
                                  :build-ids build-ids})
               sys/create-figwheel-system
               c/start)))
  (stop [this]
    (when-let [fs (:figwheel-system this)]
      (c/stop fs))
    (assoc this :figwheel-system nil))

  aproto/AssetsProvider
  (assets [this]
    (let [all-builds (if (map? all-builds)
                       (map (fn [[k v]] (assoc v :id k)) all-builds)
                       all-builds)]
      (map (fn [v]
             {:name (:serve-path v)
              :type :javascript
              :src (or (get-in v [:build-options :output-to])
                       (get-in v [:compiler :output-to]))})
           all-builds))))

(defn figwheel [conf]
  (map->Figwheel (conf/config {:figwheel conf})))
