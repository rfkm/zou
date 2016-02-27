(ns zou.web.view.selmer-test
  (:require [clojure.test :as t]
            [midje.sweet :refer :all]
            [selmer.parser :as s]
            [zou.component :as c]
            [zou.finder.proto :as fproto]
            [zou.web.view.selmer :as sut]))

(t/deftest selmer-test
  (fact
      (let [s (sut/map->SelmerFinder {:cache? false})]
        (c/with-component [s' (c/start s)]
          ((fproto/find s' "zou/web/view/test.html") ..model..) => ..ret..
          (provided
           (s/render-file "zou/web/view/test.html" ..model..) => ..ret..))))

  (fact
      (let [s (sut/map->SelmerFinder {:cache? false :base-path "zou/web/view"})]
        (c/with-component [s' (c/start s)]
          ((fproto/find s' "test.html") ..model..) => ..ret..
          (provided
           (s/render-file "zou/web/view/test.html" ..model..) => ..ret..)))))
