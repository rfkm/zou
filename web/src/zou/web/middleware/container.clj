(ns zou.web.middleware.container
  (:require [zou.web.middleware.proto :as proto]))

(defrecord Container [cargos]
  proto/RingMiddleware
  (wrap [this handler]
    (let [container (into {} cargos)]
      (fn [req]
        (handler (assoc req :zou/container container))))))
