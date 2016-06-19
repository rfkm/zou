(ns zou.component.parser-test
  (:require [zou.component.parser :as sut]
            #?(:clj [clojure.test :as t]
               :cljs [cljs.test :as t :include-macros true])))

(t/deftest gen-component-key-test
  (t/is (= :clojure.core/identity
           (#'sut/gen-component-key #'identity))))

(defn foo
  {:zou/component {:zou/dependencies {:bar :bar}}}
  [conf])

(t/deftest merge-metadata-and-conf-test
  (t/is (= (sut/parse-component-config-entry :key
                                             {:components {:key {:dependencies {:baz :baz}}}}
                                             :zou/constructor
                                             'zou.component.parser-test/foo)
           {:components {:key {:dependencies {:baz :baz
                                              :bar :bar}
                               :constructor #'zou.component.parser-test/foo}}})))
