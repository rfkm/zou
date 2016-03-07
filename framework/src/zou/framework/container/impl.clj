(ns zou.framework.container.impl
  (:require [zou.component :as c]
            [zou.framework.container.proto :as proto]
            [zou.logging :as log]
            [zou.util :as u]))

(defn get-system [a k]
  (get @a k))

(defn system-keys [a]
  (keys @a))

(defn add-system! [a k sys]
  (swap! a assoc k sys))

(defn remove-system! [a k]
  (swap! a dissoc k))

(defn start-system! [a k]
  (swap! a update k #(c/try-recovery (c/start %))))

(defn stop-system! [a k]
  (swap! a update k c/stop))

(defn start-container! [container]
  (->> container
       :system-specs
       (u/map-vals c/build-nested-system-map)
       (reduce-kv proto/add-system container)))

(defn stop-container! [container]
  (reduce (fn [c k]
            (log/infof "Stopping a system: %s" k)
            (proto/stop-system c k))
          container
          (proto/system-keys container)))

(defrecord DefaultContainer [system-specs systems]
  proto/SystemContainer
  (get-system [this system-key]
    (get-system systems system-key))
  (system-keys [this]
    (system-keys systems))
  (add-system [this system-key system]
    (add-system! systems system-key system)
    this)
  (remove-system [this system-key]
    (remove-system! systems system-key)
    this)
  (start-system [this system-key]
    (start-system! systems system-key)
    this)
  (stop-system [this system-key]
    (stop-system! systems system-key)
    this)

  c/Lifecycle
  (start [this]
    (start-container! this))
  (stop [this]
    (stop-container! this)))

(defmethod print-method DefaultContainer
  [system ^java.io.Writer writer]
  (.write writer "#<DefaultContainer")
  (doseq [k (proto/system-keys system)]
    (.write writer (str " " k)))
  (.write writer ">"))

(defn new-default-container [{:keys [system-specs]}]
  (->DefaultContainer system-specs (atom {})))
