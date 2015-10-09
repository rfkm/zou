(ns zou.web.middleware.aggregator
  (:require [zou.logging :as log]
            [zou.util :as u]
            [zou.web.context :as ctx]
            [zou.web.middleware.dependency :as dep]
            [zou.web.middleware.proto :as proto]))

(defrecord MiddlewareAggregator [middlewares dependency-map configurator inject-context-updater?]
  proto/RingMiddleware
  (wrap [this handler]
    (-> (reduce (fn [acc [k m]]
                  (if (satisfies? proto/RingMiddleware m)
                    (dep/add acc k m)
                    acc))
                {}
                middlewares)
        (dep/process-dependency-map dependency-map)
        (u/?> (ifn? configurator) (configurator this))
        (dep/sort-middlewares)
        reverse
        (u/?> inject-context-updater? (interleave (repeat [:context-updater ctx/wrap-spy])))
        (u/?> inject-context-updater? (conj [:context-updater ctx/wrap-spy]))
        (->> (reduce (fn [h [k m]]
                       (when-not (= k :context-updater)
                         (log/debugf "Applying middleware: %s" k))
                       (proto/wrap m h))
                     handler)))))
