(ns zou.web.finder.metadata
  (:require [zou.component :as c]
            [zou.logging :as log]
            [zou.util.namespace :as un]
            [zou.web.finder.proto :as proto]))

(defn- collect-vars [var-tag ns-tag]
  (if ns-tag
    (un/find-tagged-vars var-tag #(contains? (meta (the-ns %)) ns-tag))
    (un/find-tagged-vars var-tag)))

(defn- collect-conflicted-tags [pairs]
  (->> (group-by first pairs)
       (map (fn [[k v]] [k (map second v)]))
       (filter #(> (count (second %)) 1))))

(defn- warn-conflicted! [pairs]
  (doseq [[tag vars] (collect-conflicted-tags pairs)]
    (log/warn "Conflicted tags:" tag (vec vars)))
  pairs)

(defn- build-dic [vars var-tag]
  (->> vars
       (map #(vector (get (meta %) var-tag) (deref %)))
       warn-conflicted!
       (into {})))

(defrecord MetadataBasedFinder [dynamic? var-tag ns-tag]
  c/Lifecycle
  (start [this]
    (let [var-tag (or var-tag ::tag)]
      (assoc this
             :dic (build-dic (collect-vars var-tag ns-tag) var-tag)
             :var-tag var-tag)))
  (stop [this] this)

  proto/Finder
  (find [this target-key]
    (cond
      (keyword? target-key) (get (if dynamic?
                                   (build-dic (collect-vars var-tag ns-tag) var-tag)
                                   (:dic this))
                                 target-key)
      (fn? target-key)      target-key)))
