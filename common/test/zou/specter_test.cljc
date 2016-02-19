(ns zou.specter-test
  (:require #?(:clj [clojure.test :as t]
               :cljs [cljs.test :as t :include-macros true])
            [zou.specter :as sut]))

(t/deftest ext-path-test
  (let [d (atom {:a (atom {:a (atom {:a 1})})})
        p [sut/atom-path :a sut/atom-path :a sut/atom-path :a]]
    (t/are [x y] (= x y)
      (sut/select p d)        [1]
      (sut/transform p inc d) d
      (:a @(:a @(:a @d)))     2)))
