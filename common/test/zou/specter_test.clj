(ns zou.specter-test
  (:require [clojure.test :as t]
            [midje.sweet :refer :all]
            [zou.specter :as sut]))

(t/deftest ext-path-test
  (fact "atom-path"
    (let [d (atom {:a (atom {:a (atom {:a 1})})})
          p [sut/atom-path :a sut/atom-path :a sut/atom-path :a]]
      (sut/select p d) => [1]
      (sut/transform p inc d) => d
      (:a @(:a @(:a @d))) => 2)))
