(ns zou.util-test
  (:require [zou.util :as sut]
            #?(:clj [clojure.test :as t]
               :cljs [cljs.test :as t :include-macros true]))
  #?(:cljs (:require-macros [zou.util :as sut])))

(t/deftest maybe-deref-test
  (t/is (= (sut/maybe-deref (atom 100)) 100))
  (t/is (= (sut/maybe-deref 100) 100)))

(t/deftest import-test
  ;; fn
  (t/is (= (sut/map-vals inc {:a 1})
           {:a 2}))

  ;; macro
  (t/is (= (sut/letk [[a b] {:a 1 :b 2}] [a b])
           [1 2])))
