(ns zou.web.view.selmer
  (:require [selmer.parser :as s]
            [zou.component :as c]
            [zou.finder.proto :as fproto]))

;; TODO: support other options
(defrecord SelmerFinder [cache? models base-path]
  c/Lifecycle
  (start [this]
    (if (:cache? this true)
      (s/cache-on!)
      (s/cache-off!))
    (if (and (string? base-path)
             (not (.endsWith base-path "/")))
      (assoc this :base-path (str base-path "/"))
      this))
  (stop [this]
    ;; Selmer's defaualt
    (s/cache-on!)
    this)

  fproto/Finder
  (find [this s]
    (partial s/render-file (str (:base-path this) s))))
