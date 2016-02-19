(ns zou.util.namespace-test
  (:require [cljs.test :refer-macros [async deftest is testing] :as t :include-macros true]
            [zou.util.namespace :as sut :include-macros true]
            ;; fixture
            [zou.util.namespace.foo]
            [zou.util.namespace.bar]))


(t/deftest import-ns-test
  (sut/import-ns zou.util.namespace.foo)
  (t/is (= foo :foo))

  (sut/import-ns zou.util.namespace.bar #{baz})
  (t/is (= baz js/undefined)))
