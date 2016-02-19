(ns zou.util.platform-test
  (:require [zou.util.platform :as sut]
            [clojure.test :as t]
            [zou.util.platform.cljc :as cljc]))

(t/deftest if-cljs-test
  (t/is (= (sut/if-cljs :cljs :clj) :clj))
  (t/is (= (cljc/m) :clj)))
