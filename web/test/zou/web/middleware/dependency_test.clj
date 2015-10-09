(ns zou.web.middleware.dependency-test
  (:require [clojure.test :as t]
            [com.stuartsierra.dependency :as dep]
            [midje.sweet :refer :all]
            [zou.logging :as log]
            [zou.web.middleware.dependency :as sut]))

(t/deftest add-test
  (fact "add"
    (-> {}
        (sut/add :foo :bar))
    =>
    {:foo :bar})

  (fact "Show warning message if given id already exists"
    (log/with-test-logger
      (-> {:foo :bar}
          (sut/add :foo :baz))
      =>
      {:foo :baz}
      (log/logged? #"Given id ':foo' already exists" :warn)) => true))

(t/deftest depend-test
  (fact "depend"
    (-> {}
        (sut/add :foo identity)
        (sut/add :bar identity)
        (sut/depend :foo :bar)
        :foo
        meta)
    =>
    {::sut/dependencies [:bar]}))

(t/deftest dep-graph-test
  (fact "dependency-graph"
    (-> {}
        (sut/add :foo identity)
        (sut/add :bar identity)
        (sut/add :baz identity)
        (sut/depend :foo :bar)
        (sut/depend :baz :foo)
        sut/dependency-graph
        dep/topo-sort)
    =>
    [:bar :foo :baz]))

(t/deftest sort-test
  (fact "sort-middlewares"
    (-> {}
        (sut/add :foo 'foo)
        (sut/add :bar 'bar)
        (sut/add :baz 'baz)
        (sut/add :qux 'qux)
        (sut/depend :foo :bar)
        (sut/depend :baz :foo)
        sut/sort-middlewares)
    =>
    [[:bar 'bar]
     [:foo 'foo]
     [:baz 'baz]
     [:qux 'qux]]))

(t/deftest append-test
  (fact "append"
    (-> {}
        (sut/add :foo 'foo)
        (sut/add :bar 'bar)
        (sut/append :baz 'baz)
        sut/sort-middlewares
        last)
    =>
    [:baz 'baz]))

(t/deftest prepend-test
  (fact "prepend"
    (-> {}
        (sut/add :foo 'foo)
        (sut/add :bar 'bar)
        (sut/prepend :baz 'baz)
        sut/sort-middlewares
        first)
    =>
    [:baz 'baz]))

(t/deftest after-test
  (fact "after"
    (-> {}
        (sut/add :foo 'foo)
        (sut/add :bar 'bar)
        (sut/after :foo :bar)
        sut/sort-middlewares)
    =>
    [[:foo 'foo] [:bar 'bar]])

  (fact "add-after"
    (-> {}
        (sut/add :foo 'foo)
        (sut/add-after :foo :bar 'bar)
        sut/sort-middlewares)
    =>
    [[:foo 'foo] [:bar 'bar]]))

(t/deftest before-test
  (fact "before"
    (-> {}
        (sut/add :foo 'foo)
        (sut/add :bar 'bar)
        (sut/before :foo :bar)
        sut/sort-middlewares)
    =>
    [[:bar 'bar] [:foo 'foo]])

  (fact "before"
    (-> {}
        (sut/add :foo 'foo)
        (sut/add-before :foo :bar 'bar)
        sut/sort-middlewares)
    =>
    [[:bar 'bar] [:foo 'foo]]))

(t/deftest between-test
  (fact "between"
    (-> {}
        (sut/add :foo 'foo)
        (sut/add :bar 'bar)
        (sut/add :baz 'baz)
        (sut/between :foo :bar :baz)
        sut/sort-middlewares)
    =>
    [[:foo 'foo] [:baz 'baz] [:bar 'bar]])

  (fact "add-between"
    (-> {}
        (sut/add :foo 'foo)
        (sut/add :bar 'bar)
        (sut/add-between :foo :bar :baz 'baz)
        sut/sort-middlewares)
    =>
    [[:foo 'foo] [:baz 'baz] [:bar 'bar]]))

(t/deftest process-dependency-map-test
  (fact "process-dependency-map"
    (sut/process-dependency-map ..ms.. {:a {:b :before
                                            :c :after
                                            :d :invalid}
                                        :b {}})
    =>
    ..ms''..
    (provided
      (sut/before ..ms.. :b :a ) => ..ms'..
      (sut/after ..ms'.. :c :a ) => ..ms''..)))
