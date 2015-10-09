(ns zou.web.view.generic
  (:require [zou.component :as c]
            [zou.web.finder.proto :as fproto]
            [zou.web.view.proto :as proto]
            [zou.web.view.util :as vu]))

(defrecord GenericView [renderable-finder dynamic? models]
  c/Lifecycle
  (start [this]
    (let [finder (fn [view-key]
                   (fproto/find renderable-finder view-key))]
      (assoc this
             :finder (if dynamic?
                       finder
                       (memoize finder)))))
  (stop [this]
    (assoc this
           :finder nil))

  proto/View
  (show [this renderable-key model]
    (let [renderable ((:finder this) renderable-key)]
      (if (satisfies? proto/Renderable renderable)
        (->> (conj (vec (vals models)) model)
             (apply vu/merge-models)
             (proto/render renderable))
        (throw (ex-info "Found no view fn" {:renderable-key renderable-key
                                            :model model})))))

  clojure.lang.IFn
  (invoke [this view-key]
    (proto/show this view-key {}))
  (invoke [this view-key model]
    (proto/show this view-key model))
  (applyTo [this args] (clojure.lang.AFn/applyToHelper this args)))
