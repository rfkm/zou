(ns zou.util.namespace-test
  (:require [cljs.test :refer-macros [async deftest is testing] :as t :include-macros true]
            [zou.util.namespace :as sut :include-macros true]
            ;; fixture
            [zou.util.namespace.foo]
            [zou.util.namespace.bar])
  (:require-macros [zou.util.platform.m :as m]))


(t/deftest import-ns-test
  (sut/cljs-import-ns zou.util.namespace.foo)
  (t/is (= foo :foo))

  (sut/cljs-import-ns zou.util.namespace.bar #{baz})
  (t/is (= baz js/undefined)))
