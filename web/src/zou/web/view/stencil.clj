(ns zou.web.view.stencil
  (:require [clojure.core.cache :as cache]
            [stencil.core :as s]
            [stencil.loader :as sl]
            [zou.component :as c]
            [zou.finder.proto :as fproto]))

(defrecord StencilFinder [ttl base-path]
  c/Lifecycle
  (start [this]
    ;; XXX: Unfortunately, we cannot shut stencli's state into the component
    (when ttl
      (sl/set-cache (cache/ttl-cache-factory {} :ttl ttl)))
    (if (and (string? base-path)
             (not (.endsWith base-path "/")))
      (assoc this :base-path (str base-path "/"))
      this))
  (stop [this]
    (sl/invalidate-cache)
    this)

  fproto/Finder
  (find [this s]
    (partial s/render-file (str (:base-path this) s))))
