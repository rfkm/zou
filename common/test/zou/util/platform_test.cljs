(ns zou.util.platform-test
  (:require [zou.util.platform :as sut :include-macros true]
            [cljs.test :as t :include-macros true]
            [zou.util.platform.cljc :as cljc :include-macros true]))

(t/deftest if-cljs-test
  (t/is (= cljc/my-var :cljs-cljs))
  (t/is (= (sut/if-cljs :cljs :clj) :cljs))
  (t/is (= (cljc/m) :cljs)))
