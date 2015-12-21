(ns zou.web.handler-test
  (:require [clojure.test :as t]
            [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [zou.util.namespace :as un]
            [zou.web.handler :as sut]))

(def req (assoc (mock/request :get "/")
                :params {:a :aa :b {:c :d}}
                :zou/container {:view :view'}))

(t/deftest args-mapper-impl-test
  (fact
    (sut/defhandler a [a b|c |params|b $view $req $request |params :as {aa :a}]
      [a c b $view $req $request aa])
    (sut/invoke-with-mapper a req) => [:aa :d {:c :d} :view' req req :aa])
  (fact
    (sut/defhandler b
      "doc"
      {:a :b}
      [a])
    (meta #'b) => (contains {:doc "doc"
                             :a :b})))

(t/deftest defhandler-metadata-test
  (fact
    (sut/defhandler ^:foo c [])
    (contains? (meta #'c) :foo) => true)

  (fact "defhandler accepts handler name (for metadata-based finder)"
    (sut/defhandler d :d [])
    (:zou/handler (meta #'d)) => :d)

  (fact "defhandler attaches `:zou/handler-ns` tag to the current ns"
    (un/with-temp-ns [ns '((zou.web.handler/defhandler foo []))]
      (:zou/handler-ns (meta ns)) => true)))
