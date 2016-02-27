(ns zou.web.view.stencil-test
  (:require [clojure.test :as t]
            [midje.sweet :refer :all]
            [stencil.core :as s]
            [zou.component :as c]
            [zou.finder.proto :as fproto]
            [zou.web.view.stencil :as sut]))

(t/deftest stencil-test
  (fact
      (let [s (sut/map->StencilFinder {:ttl 0 })]
        (c/with-component [s' (c/start s)]
          ((fproto/find s' "zou/web/view/test.html") ..model..) => ..ret..
          (provided
           (s/render-file "zou/web/view/test.html" ..model..) => ..ret..))))

  (fact
      (let [s (sut/map->StencilFinder {:ttl 0  :base-path "zou/web/view"})]
        (c/with-component [s' (c/start s)]
          ((fproto/find s' "test.html") ..model..) => ..ret..
          (provided
           (s/render-file "zou/web/view/test.html" ..model..) => ..ret..)))))
