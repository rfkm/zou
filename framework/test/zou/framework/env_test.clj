(ns zou.framework.env-test
  (:require [clojure.test :as t]
            [environ.core :as env]
            [midje.sweet :refer :all]
            [zou.framework.env :as sut]))

(t/deftest env-test
  (facts "app-env"
    (fact
      (with-redefs [env/env {:zou-env :dev}]
        (sut/app-env)) => :dev)

    (fact "default is prod"
      (with-redefs [env/env {}]
        (sut/app-env)) => :prod)

    (fact "coerces env into keyword"
      (with-redefs [env/env {:zou-env "dev"}]
        (sut/app-env)) => :dev))

  (fact "with-env"
    (with-redefs [env/env {:baz :qux}]
      (sut/with-env {:foo :bar}
        env/env)) => {:foo :bar :baz :qux})

  (fact "with-app-env"
    (with-redefs [env/env {:zou-env :prod}]
      (sut/with-app-env :test
        (sut/app-env))) => :test)

  (fact "when-app-env"
    (sut/when-app-env :test :foo) => :foo
    (provided (sut/app-env) => :test)

    (sut/when-app-env :test :foo) => nil)

  (fact "eval-when-app-env"
    (sut/eval-when-app-env :test unkown-symbol) => (throws #"Unable to resolve symbol: unkown-symbol")
    (provided (sut/app-env) => :test)

    (sut/eval-when-app-env :test unkown-symbol) => nil))


;;; tests for automatically generated macros

(t/deftest env-generated-test
  (fact "in-prod?"
    (sut/in-prod?) => true
    (provided (sut/app-env) => :prod)

    (sut/in-prod?) => false
    (provided (sut/app-env) => :dev))

  (fact "in-dev?"
    (sut/in-dev?) => true
    (provided (sut/app-env) => :dev)

    (sut/in-dev?) => false
    (provided (sut/app-env) => :prod))

  (fact "in-test?"
    (sut/in-test?) => true
    (provided (sut/app-env) => :test)

    (sut/in-test?) => false
    (provided (sut/app-env) => :prod))

  (fact "with-prod-env"
    (sut/with-prod-env
      (sut/app-env)) => :prod)

  (fact "with-dev-env"
    (sut/with-dev-env
      (sut/app-env)) => :dev)

  (fact "with-test-env"
    (sut/with-test-env
      (sut/app-env)) => :test)

  (fact "app-env="
    (sut/app-env= :dev) => true
    (provided (sut/app-env) => :dev)

    (sut/app-env= :prod) => false
    (provided (sut/app-env) => :dev))

  (fact "when-prod-env"
    (sut/when-prod-env :foo) => :foo
    (provided (sut/app-env) => :prod)

    (sut/when-prod-env :foo) => nil
    (provided (sut/app-env) => :dev))

  (fact "when-dev-env"
    (sut/when-dev-env :foo) => :foo
    (provided (sut/app-env) => :dev)

    (sut/when-dev-env :foo) => nil
    (provided (sut/app-env) => :prod))

  (fact "when-test-env"
    (sut/when-test-env :foo) => :foo
    (provided (sut/app-env) => :test)

    (sut/when-test-env :foo) => nil)

  (fact "eval-when-prod-env"
    (sut/eval-when-prod-env :prod unkown-symbol) => (throws #"Unable to resolve symbol: unkown-symbol")
    (provided (sut/app-env) => :prod)

    (sut/eval-when-prod-env :prod unkown-symbol) => nil
    (provided (sut/app-env) => :dev))

  (fact "eval-when-dev-env"
    (sut/eval-when-dev-env :dev unkown-symbol) => (throws #"Unable to resolve symbol: unkown-symbol")
    (provided (sut/app-env) => :dev)

    (sut/eval-when-dev-env :dev unkown-symbol) => nil
    (provided (sut/app-env) => :prod))

  (fact "eval-when-test-env"
    (sut/eval-when-test-env :test unkown-symbol) => (throws #"Unable to resolve symbol: unkown-symbol")
    (provided (sut/app-env) => :test)

    (sut/eval-when-test-env :test unkown-symbol) => nil))
