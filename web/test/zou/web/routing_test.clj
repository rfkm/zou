(ns zou.web.routing-test
  (:require [clojure.test :as t]
            [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [ring.util.request :as req]
            [zou.web.routing :as sut]
            [zou.web.routing.proto :as rproto]))

(t/deftest routing-test
  (fact "routed?"
    (sut/routed? {:zou/routing {}}) => true
    (sut/routed? {}) => false)

  (facts "routed-request"
    (let [matched {:route-params {:post-id "123"}
                   :route-id :my-route
                   :handler #'identity}
          router (reify rproto/Router
                   (match [this req]
                     (when (= (req/path-info req) "/")
                       matched)))
          req (mock/request :get "/")
          not-found-req (mock/request :get "/invalid")]
      (fact
        (sut/routed-request router req) => (assoc req
                                                  :zou/routing (assoc matched
                                                                      :router router)
                                                  :route-params (:route-params matched)))

      (fact
        (sut/routed-request router not-found-req) => (assoc not-found-req
                                                            :zou/routing {:router router}))))

  (fact "routed-info"
    (sut/routed-info (assoc (mock/request :get "/") :zou/routing ..info..)) => ..info..)

  (fact "wrap-routing"
    ((sut/wrap-routing identity ..router..) ..req..) => ..req'..
    (provided
      (sut/routed-request ..router.. ..req..) => ..req'..
      (satisfies? rproto/Router ..router..) => true))

  (fact "href"
    (sut/href ..router.. ..route-id.. ..values..) => ..href..
    (provided
      (rproto/unmatch ..router.. ..route-id.. ..values..) => ..href..)))
