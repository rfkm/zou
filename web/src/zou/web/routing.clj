(ns zou.web.routing
  (:require [zou.web.middleware.proto :as mproto]
            [zou.web.routing.proto :as proto]))

(defn routed? [req]
  (contains? req :zou/routing))

(defn routed-request [router req]
  {:pre [(satisfies? proto/Router router)]}
  (if-let [matched (proto/match router req)]
    (assoc req
           :zou/routing (assoc matched
                               :router router)
           :route-params (:route-params matched))
    req))

(defn routed-info [req]
  (:zou/routing req))

(defn route-id [req]
  (:route-id (routed-info req)))

(defn invoke-handler [router req not-found-fn]
  {:pre [(satisfies? proto/Router router)]}
  (let [req (if (routed? req)
              req
              (routed-request router req))]
    (if-let [h (:handler (routed-info req))]
      (h req)
      (not-found-fn req))))

(defn wrap-routing
  "A middleware that inject routing info into a request map. It is not
  mandatory for the routing because `invoke-handler` tries finding a
  route. However, it allows other middlewares to use routing
  information by injecting it into request map at an early stage."
  [handler router]
  {:pre [(satisfies? proto/Router router)]}
  (fn routing [req]
    (handler (routed-request router req))))

(defrecord RoutingMiddleware [router]
  mproto/RingMiddleware
  (wrap [this handler]
    (wrap-routing handler router)))

(defn href
  ([router route-id]
   (href router route-id {}))
  ([router route-id values]
   (proto/unmatch router route-id values)))
