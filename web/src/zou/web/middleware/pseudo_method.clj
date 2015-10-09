(ns zou.web.middleware.pseudo-method
  (:require [clojure.string :as str]))

(defn wrap-pseudo-method [handler]
  (fn [req]
    (let [request-method (:request-method req)
          form-method    (or (get-in req [:form-params "_method"])
                             (get-in req [:multipart-params "_method"]))]
      (if (and form-method (= request-method :post))
        (handler (-> req
                     (assoc :request-method
                            (keyword (str/lower-case form-method)))
                     (assoc :original-request-method
                            request-method)))
        (handler req)))))
