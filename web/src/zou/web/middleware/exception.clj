(ns zou.web.middleware.exception
  (:require [ring.util.http-response :as res]
            [ring.util.http-status :as status]
            [slingshot.slingshot :as ss]
            [zou.logging :as log]
            [zou.web.context :as ctx]
            [zou.web.middleware.proto :as proto]))

;;; TODO: Improve error handling

(defn- error-body [name description]
  (format "<h1>%s</h1><p>%s</p>" name description))

(defn default-error-handler [req ex]
  (log/error (ex-info "Uncaught exception" {:request req} ex)
             "Uncaught exception")
  (-> (res/internal-server-error (error-body (status/get-name 500)
                                             (status/get-description 500)))
      (res/content-type "text/html")))

(defn wrap-response-exception-handler
  "A middleware that handles an exception thrown by
  `ring.util.http-response/throw!`."
  [handler]
  (fn [req]
    (ss/try+
     (handler req)
     (catch [:type ::res/response] {:keys [response]}
       (if (some? (:body response))
         response
         (-> response
             (assoc :body (error-body (status/get-name (:status response))
                                      (status/get-description (:status response))))
             (res/content-type "text/html")))))))

(defrecord ErrorHandler [request-context error-handler]
  proto/RingMiddleware
  (wrap [this handler]
    (fn [req]
      (try
        (handler req)
        (catch Throwable e
          ((or error-handler default-error-handler)
           (or (ctx/current-request request-context) req)
           e))))))
