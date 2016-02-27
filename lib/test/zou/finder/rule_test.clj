(ns zou.finder.rule-test
  (:require [clojure.test :as t]
            [midje.sweet :refer :all]
            [zou.component :as c]
            [zou.finder.proto :as proto]
            [zou.finder.rule :as sut]))

(def test-var)

(t/deftest helper-test
  (fact "expand-kw"
    (let [conf {nil :hoge.handler
                :acme :acme.handler
                :acme.fuga :acme-fuga.handler}]
      (sut/expand-kw :foo/bar conf) => :hoge.handler.foo/bar
      (sut/expand-kw :acme.foo/bar conf) => :acme.handler.foo/bar
      ;; longer match wins
      (sut/expand-kw :acme.fuga/bar conf) => :acme-fuga.handler/bar

      (sut/expand-kw :bar {nil :hoge.handler
                           :bar :baz}) => :hoge.handler/bar))

  (fact "kw->var"
    (sut/kw->var ::test-var {}) => #'test-var
    (sut/kw->var :foo/test-var {:foo :zou.finder.rule-test}) => #'test-var))

(t/deftest component-test
  (fact
      (c/with-component [c (sut/->RuleBasedFinder {:foo :zou.finder.rule-test})]
        (proto/find c :foo/test-var) => #'test-var)))
