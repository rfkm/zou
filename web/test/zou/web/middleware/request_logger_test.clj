(ns zou.web.middleware.request-logger-test
  (:require [clojure.test :as t]
            [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [zou.logging :as log]
            [zou.web.middleware.proto :as proto]
            [zou.web.middleware.request-logger :as sut]))

(t/deftest request-logger-test
  (facts
    (fact
      (log/with-test-logger
        (let [req (mock/request :get "/foo")]
          ((proto/wrap (sut/map->RequestLogger {:printer :no-color}) identity) req) => (contains req)
          (log/logged? #"Starting :get /foo for localhost" :info) => true
          (log/logged? #"Finished :get /foo for localhost" :info) => true
          (log/logged? #"Request details" :debug) => true)))

    (fact
      (log/with-test-logger
        (let [req (mock/request :post "/foo" {:a :a})]
          ((proto/wrap (sut/map->RequestLogger {:printer :no-color
                                                :log-body? true}) identity) req)
          => anything
          (log/logged? #"Starting :post /foo for localhost" :info) => true
          (log/logged? #"Finished :post /foo for localhost" :info) => true
          (log/logged? #"Request details" :debug) => true
          (log/logged? #"Raw request body" :debug) => true)))

    (fact
      (log/with-test-logger
        (let [req (mock/request :get "/foo/bar")]
          ((proto/wrap
            (sut/map->RequestLogger {:printer :no-color
                                     :blacklist-pattern #"^/foo.*"})
            identity) req)
          =>
          req
          (log/not-logged?) => true)))

    (fact
      (log/with-test-logger
        (let [req (mock/request :get "/foo/bar")]
          ((proto/wrap
            (sut/map->RequestLogger {:printer :no-color
                                     :whitelist-pattern #"^/bar.*"})
            identity) req)
          =>
          req

          (log/not-logged?) => true)))))
