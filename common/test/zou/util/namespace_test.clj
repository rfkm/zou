(ns zou.util.namespace-test
  (:require [bultitude.core :as b]
            [clojure.test :as t]
            [midje.sweet :refer :all]
            [zou.util.namespace :as sut]))

(fact "with-temp-ns"
  (fact "basic"
    (sut/with-temp-ns [a '((def a (+ 1 1))
                           (def b (+ a 1)))
                       b '((def c (+ 1 1))
                           (def d (+ c 1)))]
      @(ns-resolve (ns-name a) 'a) => 2
      @(ns-resolve (ns-name a) 'b) => 3
      @(ns-resolve (ns-name b) 'c) => 2
      @(ns-resolve (ns-name b) 'd) => 3))

  (fact "Ensure temp ns is deleted"
    (sut/with-temp-ns [a "myns" '()])
    (the-ns 'myns) => (throws #"No namespace: myns found"))

  (fact "Can specify namespace name"
    (sut/with-temp-ns [a "tempns1" '((def a :a)
                                     (def b :b))
                       b '((def c :c)
                           (def d :d))
                       c "tempns2" '((def e :e)
                                     (def f :f))]
      @(ns-resolve 'tempns1 'a) => :a
      @(ns-resolve 'tempns1 'b) => :b
      @(ns-resolve (ns-name b) 'c) => :c
      @(ns-resolve (ns-name b) 'd) => :d
      @(ns-resolve 'tempns2 'e) => :e
      @(ns-resolve 'tempns2 'f) => :f)))

(t/deftest resolve-test
  (fact "resolve"
    (sut/resolve-var 'foo/bar) => ..var..
    (provided
      (require 'foo) => nil
      (ns-resolve 'foo 'bar) => ..var..)

    (sut/resolve-var 'bar) => ..var..
    (provided
      (ns-resolve *ns* 'bar) => ..var..)

    (sut/resolve-var 'identity) => #'clojure.core/identity
    (sut/resolve-var 'invalid-ns/foo) => (throws #"Could not locate")
    (sut/resolve-var 'clojure.core/foo) => (throws #"Unable to resolve var: clojure.core/foo in this context")))

(t/deftest require-test
  (fact "require-all"
    (sut/require-all nil "foo") => nil
    (provided
      (b/namespaces-on-classpath :prefix "foo") => (list 'foo.bar 'foo.baz)
      (require 'foo.bar) => nil
      (require 'foo.baz) => nil))

  (fact "require-all w/ classpath"
    (sut/require-all "test" "zou.util") => nil
    (provided
      (b/namespaces-on-classpath :prefix "zou.util" :classpath "test") => (list 'foo.bar 'foo.baz)
      (require 'foo.bar) => nil
      (require 'foo.baz) => nil)))

(t/deftest contains-tagged-var-test
  (facts "contains-tagged-var?"
    (fact
      (sut/with-temp-ns [ns '()]
        (sut/contains-tagged-var? ns :foo) => false))

    (fact
      (sut/with-temp-ns [ns '((def ^:foo bar))]
        (sut/contains-tagged-var? ns :foo)) => true)

    (fact
      (sut/with-temp-ns [ns '((def ^:bar bar))]
        (sut/contains-tagged-var? ns :foo))=> false)

    (fact
      (sut/with-temp-ns [ns '((def ^:private ^:foo bar))]
        (sut/contains-tagged-var? ns :foo)) => true)))

(t/deftest tagged-ns?-test
  (facts "tagged-ns?"
    (fact
      (sut/with-temp-ns [ns '()]
        (sut/tagged-ns? ns :foo) => false))

    (fact
      (sut/with-temp-ns [ns '((alter-meta! *ns* assoc :foo true))]
        (sut/tagged-ns? ns :foo))
      => true)

    (fact
      (sut/with-temp-ns [ns '((alter-meta! *ns* assoc :foo false))]
        (sut/tagged-ns? ns :foo))
      => false)))

(t/deftest find-ns-contains-tagged-var-test
  (facts "find-ns-contains-tagged-var"
    (sut/with-temp-ns [ns1 '((def ^:foo foo))
                       ns2 '((def ^:bar bar))]
      (sut/find-ns-contains-tagged-var [:foo]) => [(ns-name ns1)]
      (sut/find-ns-contains-tagged-var [:bar]) => [(ns-name ns2)]
      (sut/find-ns-contains-tagged-var [:foo :bar]) => (just [(ns-name ns1) (ns-name ns2)] :in-any-order))))

(t/deftest find-tagged-ns-test
  (facts "find-tagged-ns"
    (sut/with-temp-ns [ns1 '((alter-meta! *ns* assoc :foo true))
                       ns2 '((alter-meta! *ns* assoc :bar true))]
      (sut/find-tagged-ns [:foo]) => [(ns-name ns1)]
      (sut/find-tagged-ns [:bar]) => [(ns-name ns2)]
      (sut/find-tagged-ns [:foo :bar]) => (just [(ns-name ns1) (ns-name ns2)] :in-any-order))))

(t/deftest find-tagged-vars-test
  (facts "find-tagged-vars"
    (sut/with-temp-ns [ns1 '((def ^:foo foo)
                             (def ^:bar bar))
                       ns2 '((def ^:private ^:foo foo))]
      (try
        (sut/find-tagged-vars :foo) => (just [(ns-resolve (ns-name ns1) 'foo) (ns-resolve (ns-name ns2) 'foo)] :in-any-order)
        (sut/find-tagged-vars :foo #(= % ns1)) => (just [(ns-resolve (ns-name ns1) 'foo)])))))
