(ns zou.web.middleware.request-logger
  (:require [ring.logger :as logger]
            [ring.middleware.conditional :as c]
            [zou.util :as u]
            [zou.web.middleware.proto :as proto]))

(def ^:private original-available-options
  [:logger :printer :timing :exceptions :redact-fn :redact-keys :redact-value])

(defn- if-url-doesnt-match [regex middleware]
  #(c/if-url-doesnt-match % regex middleware))

(defn- if-url-matches [regex middleware]
  #(c/if-url-matches % regex middleware))

(defrecord RequestLogger [log-body? blacklist-pattern whitelist-pattern]
  proto/RingMiddleware
  (wrap [this handler]
    (let [opts (select-keys (into {} this)
                            original-available-options)
          m (->> #(logger/wrap-with-logger % opts)
                 (u/?>> log-body? (comp logger/wrap-with-body-logger))
                 (u/?>> blacklist-pattern (if-url-doesnt-match blacklist-pattern))
                 (u/?>> whitelist-pattern (if-url-matches whitelist-pattern)))]
      (m handler))))
