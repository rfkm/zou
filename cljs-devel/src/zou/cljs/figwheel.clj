(ns zou.cljs.figwheel
  (:require [figwheel-sidecar.config :as conf]
            [figwheel-sidecar.system :as sys]
            [zou.component :as c]
            [zou.util.namespace :as un]
            [zou.web.asset.proto :as aproto]))

(un/import-vars [figwheel-sidecar.components.css-watcher css-watcher])
(un/import-vars [figwheel-sidecar.system cljs-repl])

(defn- extract-build-conf [{:keys [builds]}]
  (:all-builds (conf/prep-config (conf/config {:cljsbuild {:builds builds}}))))

(defrecord Figwheel [enable-server? figwheel-options all-builds build-ids cljsbuild]
  c/Lifecycle
  (start [this]
    (if enable-server?
      (assoc this
             :figwheel-system
             (-> (conf/prep-config {:figwheel-options figwheel-options
                                    :all-builds (concat all-builds
                                                        (extract-build-conf cljsbuild))
                                    :build-ids build-ids})
                 sys/create-figwheel-system
                 c/start))
      this))
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
  (let [enable-server? (:enable-server? conf true)
        conf {:figwheel (dissoc conf :enable-server?)}]
    (map->Figwheel (assoc (conf/config conf)
                          :enable-server? enable-server?))))
