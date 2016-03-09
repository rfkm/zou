(ns zou.framework.container.impl
  (:require [zou.component :as c]
            [zou.framework.container.proto :as proto]
            [zou.logging :as log]
            [zou.util :as u]))

(defn get-component [a k]
  (get @a k))

(defn as-system [a]
  @a)

(defn component-keys [a]
  (keys @a))

(defn add-component! [a k component]
  (swap! a assoc k component))

(defn remove-component! [a k]
  (swap! a dissoc k))

(defn start-system! [a]
  (swap! a #(c/try-recovery (c/start %))))

(defn stop-system! [a]
  (swap! a c/stop))

(defn- transitive-dep-keys [system k]
  (letfn [(f [k]
            (if-let [dep-keys (seq (vals (c/dependencies (get system k))))]
              (conj (mapcat f dep-keys) k)
              [k]))]
    (f k)))

(defn- narrow-system [system ks]
  (c/map->SystemMap (select-keys system (reduce into #{} (map #(transitive-dep-keys system %) ks)))))

(defrecord DefaultContainer []
  proto/ComponentContainer
  (get-component [this component-key]
    (get-component (::system this) component-key))
  (as-system [this]
    (as-system (::system this)))
  (component-keys [this]
    (component-keys (::system this)))
  (add-component [this component-key component]
    (add-component! (::system this) component-key component)
    this)
  (remove-component [this component-key]
    (remove-component! (::system this) component-key)
    this)
  (start-system [this]
    (start-system! (::system this))
    this)
  (stop-system [this]
    (stop-system! (::system this))
    this)
  (narrow [this ks]
    (narrow-system (proto/as-system this) ks))

  c/Lifecycle
  (start [this]
    (let [sys-a (::system this (atom nil))]
      (reset! sys-a (-> this
                        (dissoc ::system)
                        c/build-nested-system-map))
      (assoc this ::system sys-a)))
  (stop [this]
    (proto/stop-system this)))

(defmethod print-method DefaultContainer
  [container ^java.io.Writer writer]
  (.write writer "#<DefaultContainer")
  (doseq [k (proto/component-keys container)]
    (.write writer (str " " k)))
  (.write writer ">"))

(defn new-default-container [system-spec]
  (map->DefaultContainer (assoc system-spec ::system (atom {}))))
