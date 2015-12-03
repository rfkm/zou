(ns zou.web.routing.bidi
  (:require [bidi.bidi :as bidi]
            [cemerick.url :as url]
            [clojure.walk :as walk]
            [ring.util.request :as req]
            [zou.component :as c]
            [zou.util :as u]
            [zou.web.finder.proto :as fproto]
            [zou.web.middleware.proto :as mproto]
            [zou.web.routing.proto :as proto]))

(declare unmatch)

(defrecord BidiRouter [route-providers handler-finder middlewares dynamic?]
  c/Lifecycle
  (start [this]
    (let [finder (fn [route-id]
                   (when-let [h (fproto/find handler-finder route-id)]
                     (if (satisfies? mproto/RingMiddleware middlewares)
                       (mproto/wrap middlewares h)
                       h)))]
      (assoc this
             :routes (bidi/routes this)
             :finder (if dynamic?
                       finder
                       (memoize finder)))))
  (stop [this]
    (assoc this
           :routes nil
           :finder nil))

  bidi/RouteProvider
  (routes [this]
    ["" (vec (for [[c r] route-providers
                   :when (satisfies? bidi/RouteProvider r)
                   :let [ctx (:route-context r)]] ; XXX: should avoid assuming that route provider is associative?
               [(or ctx "") [(bidi/routes r)]]))])

  proto/Router
  (match [this req]
    (when-let [{route-id :handler
                params :route-params}
               (bidi/match-route*
                ;; Builds routing table per request if `dynamic?` is
                ;; true. It could be slow but useful during development.
                (if dynamic? (bidi/routes this) (:routes this))
                (req/path-info req)
                req)]
      {:route-params params
       :route-id route-id
       :handler (if (fn? route-id)
                  route-id
                  ((:finder this) route-id))}))

  (unmatch [this route-id params]
    (unmatch (bidi/routes this) route-id params)))

(defn- get-path [route route-id]
  (:path (first (filter #(= (:handler %) route-id) (bidi/route-seq route)))))

(defn- extract-route-params [pattern]
  (when (vector? pattern)
    (map bidi/param-key pattern)))

(defn- param-keys [route route-id]
  (->> route-id
       (get-path route)
       (map extract-route-params)
       (apply concat)
       (remove nil?)))

(defn unmatch [routes route-id params]
  (let [query-params (apply dissoc params (param-keys routes route-id))]
    (str
     (u/mapply bidi/path-for routes
               (if (string? route-id)
                 (keyword route-id)
                 route-id)
               params)
     (when (seq query-params)
       (str "?" (url/map->query query-params))))))

(defn portable-routes
  "[Experimental] Makes a routes definition portable between clj and cljs.

  It doesn't allow you to match routes to get handlers but generate a
  path by using bidi.bidi/path-for in CLJS. This is intended to be
  used within a macro."
  [routes]
  (walk/postwalk (fn [x]
                   (cond
                     (= x long) 'long
                     (= x bidi.bidi/uuid) 'bidi.bidi/uuid
                     (= x keyword) 'keyword
                     (fn? x) :omitted   ; give up
                     :else x))
                 routes))
