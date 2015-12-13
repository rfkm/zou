(ns zou.web.middleware.pseudo-method-test
  (:require [clojure.test :as t]
            [midje.sweet :refer :all]
            [ring.middleware.defaults :as default]
            [ring.mock.request :as mock]
            [zou.web.middleware.pseudo-method :as sut]))

(t/deftest pseudo-method-middleware-test
  (facts "wrap-pseudo-method"
    (let [f (default/wrap-defaults (sut/wrap-pseudo-method identity) default/api-defaults)]
      (fact "it works"
        (f (-> (mock/request :post "/")
               (mock/body {:_method "DELETE"}))) => (contains {:request-method :delete
                                                               :original-request-method :post}))

      (fact "only works with post method"
        (f (-> (mock/request :get "/")
               (mock/body {:_method "DELETE"}))) => (contains {:request-method :get})))))
