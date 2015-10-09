(ns zou.web.middleware.proto)

(defprotocol RingMiddleware
  (wrap [middleware handler]))

(extend-protocol RingMiddleware
  clojure.lang.Fn
  (wrap [f handler]
    (f handler))

  clojure.lang.Var
  (wrap [var handler]
    (wrap (deref var) handler))

  clojure.lang.APersistentMap
  (wrap [{:keys [fn args]} handler]
    (apply fn handler args)))
