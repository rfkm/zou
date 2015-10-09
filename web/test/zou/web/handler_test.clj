(ns zou.web.handler-test
  (:require [clojure.test :as t]
            [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [zou.web.handler :as sut]))

(def req (assoc (mock/request :get "/")
                :params {:a :aa}
                :zou/container {:view :view'}))

(t/deftest args-mapper-impl-test
  (fact
    (sut/defhandler a [a $view $req $request]
      [a $view $req $request])

    (sut/invoke-with-mapper a req) => [:aa :view' req req])
  (fact
    (sut/defhandler b
      "doc"
      {:a :b}
      [a])
    (meta #'b) => (contains {:doc "doc"
                             :a :b})))
