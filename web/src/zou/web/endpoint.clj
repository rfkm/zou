(ns zou.web.endpoint
  (:require [zou.util :as u]
            [zou.web.context :as ctx]
            [zou.web.handler.proto :as h]
            [zou.web.middleware.proto :as m]
            [zou.web.response :as res]
            [zou.web.routing :as routing]))

(defn not-found-fn [req]
  (res/not-found!))

(defrecord Endpoint [middlewares router request-logger]
  h/RingHandler
  (handler [this]
    (let [endpoint (fn endpoint [req]
                     (routing/invoke-handler router req not-found-fn))
          endpoint (if (satisfies? m/RingMiddleware middlewares)
                     (m/wrap middlewares endpoint)
                     endpoint)]
      (->> endpoint
           ctx/wrap-initial-context
           (u/?>> request-logger (m/wrap request-logger))))))
