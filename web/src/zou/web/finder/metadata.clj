(ns zou.web.finder.metadata
  (:require [zou.component :as c]
            [zou.logging :as log]
            [zou.util.namespace :as un]
            [zou.web.finder.proto :as proto]))

(defn- collect-vars [tag-name]
  (un/find-tagged-vars tag-name))

(defn- collect-conflicted-tags [pairs]
  (->> (group-by first pairs)
       (map (fn [[k v]] [k (map second v)]))
       (filter #(> (count (second %)) 1))))

(defn- warn-conflicted! [pairs]
  (doseq [[tag vars] (collect-conflicted-tags pairs)]
    (log/warn "Conflicted tags:" tag (vec vars)))
  pairs)

(defn- build-dic [vars tag-name]
  (->> vars
       (map #(vector (get (meta %) tag-name) (deref %)))
       warn-conflicted!
       (into {})))

(defrecord MetadataBasedFinder [dynamic? tag-name]
  c/Lifecycle
  (start [this]
    (let [tag-name (or tag-name ::tag)]
      (assoc this
             :dic (build-dic (collect-vars tag-name) tag-name)
             :tag-name tag-name)))
  (stop [this] this)

  proto/Finder
  (find [this target-key]
    (cond
      (keyword? target-key) (get (if dynamic?
                                   (build-dic (collect-vars tag-name) tag-name)
                                   (:dic this))
                                 target-key)
      (fn? target-key)      target-key)))
