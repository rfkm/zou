(ns zou.web.middleware
  (:refer-clojure :exclude [error-handler])
  (:require [prone.middleware :as prone]
            [ring.middleware.defaults :as defaults]
            [ring.middleware.stacktrace :as stacktrace]
            [zou.web.middleware.exception :as ex]
            [zou.web.middleware.out :as out]
            [zou.web.middleware.proto :as proto]
            [zou.web.middleware.pseudo-method :as pmethod]
            [zou.web.middleware.reload :as reload]
            [zou.web.routing :as routing]))

(defn simple-middleware [f conf]
  (reify proto/RingMiddleware
    (wrap [this handler]
      (if (seq conf)
        (f handler conf)
        (f handler)))))

(def stacktrace-web (partial simple-middleware stacktrace/wrap-stacktrace-web))
(def stacktrace-log (partial simple-middleware stacktrace/wrap-stacktrace-log))
(def prone (partial simple-middleware prone/wrap-exceptions))
(def defaults (partial simple-middleware defaults/wrap-defaults))
(def nrepl-out-bridge (partial simple-middleware out/wrap-nrepl-out-bridge))
(def response-exception-handler (partial simple-middleware ex/wrap-response-exception-handler))
(def error-handler ex/map->ErrorHandler)
(def reload (partial simple-middleware reload/wrap-reload))
(def pseudo-method (partial simple-middleware pmethod/wrap-pseudo-method))
