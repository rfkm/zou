(ns zou.web.immutant-web
  (:require [immutant.web :as immutant]
            [zou.component :as c]
            [zou.logging :as log]
            [zou.web.handler.proto :as h]))

(defn default-handler [req]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body "Running"})

(defn- get-handler-fn [handler]
  (assert (satisfies? h/RingHandler handler))
  (h/handler handler))

(defrecord WebServer [handler port path]
  c/Lifecycle
  (start [this]
    (log/infof "Starting web server on port %d with context %s" port (or path "/"))
    (let [conf (select-keys this (:valid-options (meta #'immutant/run)))]
      (assoc this
             :server (immutant/run (or (and handler (get-handler-fn handler))
                                       default-handler)
                       conf))))
  (stop [this]
    (log/infof "Stopping web server on port %d with context %s" port (or path "/"))
    (if-let [server (:server this)]
      (do (immutant/stop server)
          (assoc this :server nil))
      this)))
