(ns zou.framework.container
  (:require [zou.framework.container.proto :as proto]
            [zou.component :as c]))

(defn system [container system-key]
  (proto/get-system container system-key))

(defn systems
  ([container]
   (->> (proto/system-keys container)
        (map #(vector % (proto/get-system container %)))
        (into {}))))

(defn add-system! [container system-key system]
  (proto/add-system container system-key system))

(defn add-systems! [container systems]
  (reduce-kv add-system! container systems))

(defn start-system! [container system-key]
  (proto/start-system container system-key))

(defn start-systems! [container]
  (reduce start-system! container (proto/system-keys container)))

(defn stop-system! [container system-key]
  (proto/stop-system container system-key))

(defn stop-systems! [container]
  (reduce stop-system! container (proto/system-keys container)))

(defn remove-system! [container system-key]
  (proto/remove-system container system-key))

(defn load-system! [container conf-map system-key]
  (->> conf-map
       c/build-nested-system-map
       (add-system! container system-key)))

(defn load-systems! [container system-specs]
  (reduce-kv load-system! container system-specs))
