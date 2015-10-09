(ns zou.util.namespace-test
  (:require [clojure.test :as t]
            [hara.namespace.eval :as ne]
            [midje.sweet :refer :all]
            [zou.util.namespace :as sut]))

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
    (sut/require-all "zou.util") => nil
    (provided
      (require (as-checker #(.startsWith (name %) "zou.util"))) => nil)))

(t/deftest contains-tagged-var-test
  (facts "contains-tagged-var?"
    (fact
      (ne/with-temp-ns
        (zou.util.namespace/contains-tagged-var? *ns* :foo))
      => false)

    (fact
      (ne/with-temp-ns
        (def ^:foo bar)
        (zou.util.namespace/contains-tagged-var? *ns* :foo))
      => true)

    (fact
      (ne/with-temp-ns
        (def ^:bar bar)
        (zou.util.namespace/contains-tagged-var? *ns* :foo))
      => false)

    (fact
      (ne/with-temp-ns
        (def ^:private ^:foo bar)
        (zou.util.namespace/contains-tagged-var? *ns* :foo))
      => true)))

(t/deftest tagged-ns?-test
  (facts "tagged-ns?"
    (fact
      (ne/with-temp-ns
        (zou.util.namespace/tagged-ns? *ns* :foo))
      => false)

    (fact
      (ne/with-temp-ns
        (alter-meta! *ns* assoc :foo true)
        (zou.util.namespace/tagged-ns? *ns* :foo))
      => true)

    (fact
      (ne/with-temp-ns
        (alter-meta! *ns* assoc :foo false)
        (zou.util.namespace/tagged-ns? *ns* :foo))
      => false)))

(t/deftest find-ns-contains-tagged-var-test
  (facts "find-ns-contains-tagged-var"
    (let [ns1 (gensym)
          ns2 (gensym)]
      (try
        (create-ns ns1)
        (create-ns ns2)
        (ne/eval-ns ns1 '((def ^:foo foo)))
        (ne/eval-ns ns2 '((def ^:bar bar)))
        (sut/find-ns-contains-tagged-var [:foo]) => [ns1]
        (sut/find-ns-contains-tagged-var [:bar]) => [ns2]
        (sut/find-ns-contains-tagged-var [:foo :bar]) => (just [ns1 ns2] :in-any-order)
        (finally (remove-ns ns1)
                 (remove-ns ns2))))))

(t/deftest find-tagged-ns-test
  (facts "find-tagged-ns"
    (let [ns1 (gensym)
          ns2 (gensym)]
      (try
        (create-ns ns1)
        (create-ns ns2)
        (ne/eval-ns ns1 '((clojure.core/refer-clojure) (alter-meta! *ns* assoc :foo true)))
        (ne/eval-ns ns2 '((clojure.core/refer-clojure) (alter-meta! *ns* assoc :bar true)))
        (sut/find-tagged-ns [:foo]) => [ns1]
        (sut/find-tagged-ns [:bar]) => [ns2]
        (sut/find-tagged-ns [:foo :bar]) => (just [ns1 ns2] :in-any-order)
        (finally (remove-ns ns1)
                 (remove-ns ns2))))))

(t/deftest find-tagged-vars-test
  (facts "find-tagged-vars"
    (let [ns1 (gensym)
          ns2 (gensym)]
      (try
        (create-ns ns1)
        (create-ns ns2)
        (ne/eval-ns ns1 '((def ^:foo foo)
                          (def ^:bar bar)))
        (ne/eval-ns ns2 '((def ^:private ^:foo foo)))
        (sut/find-tagged-vars :foo) => (just [(ns-resolve ns1 'foo) (ns-resolve ns2 'foo)] :in-any-order)
        (finally (remove-ns ns1)
                 (remove-ns ns2))))))
