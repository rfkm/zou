(ns zou.web.view.generic-test
  (:require [clojure.test :as t]
            [midje.sweet :refer :all]
            [zou.component :as c]
            [zou.web.finder.proto :as fproto]
            [zou.web.view.generic :as sut]
            [zou.web.view.proto :as vproto]))

(t/deftest generate-view-test
  (facts
    (let [tbl {:index identity}]
      (c/with-system [c {:view   {:zou/constructor sut/map->GenericView
                                  :zou/dependencies {:renderable-finder :finder}
                                  :zou/optionals {:models :tag/view-model}
                                  :dynamic? true}
                         :finder {:zou/constructor (constantly (reify fproto/Finder
                                                                 (find [this k] (get tbl k))))}
                         :model-a {:zou/tags [:tag/view-model]
                                   :ak :av}}]
        (vproto/show (:view c) :index {:foo :bar}) => {:foo :bar :ak :av}
        (vproto/show (:view c) :index {:ak :av'}) => {:ak :av'} ; can override
        (vproto/show (:view c) :invalid ..model..) => (throws "Found no view fn")

        ;; callable
        ((:view c) :index)  => {:ak :av}
        ((:view c) :index {:foo :bar})  => {:foo :bar :ak :av}
        (apply (:view c) [:index {:foo :bar}])  => {:foo :bar :ak :av}))))
