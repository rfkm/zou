(ns zou.util.platform-test
  (:require [zou.util.platform :as sut]
            [clojure.test :as t]))

(t/deftest if-cljs-test
  (t/is (= (sut/if-cljs :cljs :clj) :clj)))
