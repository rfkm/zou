(ns zou.util.platform-test
  (:require [zou.util.platform :as sut :include-macros true]
            [cljs.test :as t :include-macros true]))

(t/deftest if-cljs-test
  (t/is (= (sut/if-cljs :cljs :clj) :cljs)))
