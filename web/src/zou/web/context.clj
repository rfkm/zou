(ns zou.web.context)

(declare ^:private ^:dynamic *context*)

(defn- context []
  *context*)

(defn- request []
  (when (bound? #'*context*)
    (:request *context*)))

(defn- set-context! [ctx]
  (when (bound? #'*context*)
    (set! *context* ctx)))

(defn- update-in! [ks f & args]
  (when (bound? #'*context*)
    (set-context! (apply update-in *context* ks f args))))

(defn- set-request! [req]
  (update-in! [:request] (constantly req)))

(defmacro ^:private with-context [ctx & body]
  `(binding [*context* ~ctx]
     ~@body))

(defmacro ^:private with-initial-context [& body]
  `(with-context {}
     ~@body))

(defn wrap-initial-context [handler]
  (fn [req]
    (with-initial-context
      (handler req))))

(defn wrap-spy [handler]
  (fn [req]
    (when-not (identical? req (request))
      (set-request! req))
    (handler req)))

(defprotocol RequestAccessor
  (current-request [ctx] ))

(defrecord RequestContext []
  RequestAccessor
  (current-request [ctx]
    (request)))
