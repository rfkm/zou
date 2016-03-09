(ns zou.framework.container
  (:require [zou.framework.container.proto :as proto]
            [zou.component :as c]))

(defn component [container component-key]
  (proto/get-component container component-key))

(defn system
  ([container]
   (proto/as-system container)))

(defn- transitive-dep-keys [system k]
  (letfn [(f [k]
            (if-let [dep-keys (seq (vals (c/dependencies (get system k))))]
              (conj (mapcat f dep-keys) k)
              [k]))]
    (f k)))

(defn- narrow-system [system & ks]
  (select-keys system (reduce into #{} (map #(transitive-dep-keys system %) ks))))

(defn subsystem [container & ks]
  (apply narrow-system (system container) ks))

(defn start-system! [container]
  (proto/start-system container))

(defn stop-system! [container]
  (proto/stop-system container))
