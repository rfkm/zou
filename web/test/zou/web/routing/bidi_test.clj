(ns zou.web.routing.bidi-test
  (:require [bidi.bidi :as bidi]
            [clojure.test :as t]
            [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [zou.component :as c]
            [zou.web.routing.bidi :as sut]
            [zou.web.routing.proto :as rproto]))


(t/deftest bidi-router-test
  (c/with-component [r (sut/map->BidiRouter {:route-providers {:a (reify bidi/RouteProvider
                                                                    (routes [this] ["" {"/foo" :foo}]))
                                                               :b (reify bidi/RouteProvider
                                                                    (routes [this] ["" {["/bar/" :baz] :bar}]))}
                                             :handler-finder {:foo :foo'
                                                              :bar :bar'} ; PersistentMap has impl of Finder protocol
                                             :dynamic? true})]
    (fact
      (bidi/routes r) => ["" [["" [["" {"/foo" :foo}]]]
                              ["" [["" {["/bar/" :baz] :bar}]]]]])

    (fact
      (rproto/match r (mock/request :get "/foo")) => {:handler :foo'
                                                      :route-id :foo
                                                      :route-params nil}
      (rproto/match r (mock/request :get "/bar/100")) => {:handler :bar'
                                                          :route-id :bar
                                                          :route-params {:baz "100"}})

    (fact
      (rproto/unmatch r :foo {}) => "/foo"
      (rproto/unmatch r :bar {:baz 200}) => "/bar/200"
      (rproto/unmatch r :bar {:baz 200 :qux 300}) => "/bar/200?qux=300")))
