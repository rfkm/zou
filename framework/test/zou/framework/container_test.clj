(ns zou.framework.container-test
  (:require [clojure.test :as t]
            [midje.sweet :refer :all]
            [zou.component :as c]
            [zou.framework.container :as sut]
            [zou.framework.container.impl :as impl]))

(t/deftest container-test
  (fact "container"
    (let [container (c/start (impl/new-default-container {}))]
      ;; lifecycle test
      (fact "add"
        (sut/add-system! container :key ..sys..) => container
        (sut/system container :key) => ..sys..)

      (fact "start"
        (sut/start-system! container :key) => container
        (provided
         (c/start ..sys..) => ..sys'..)
        (sut/system container :key) => ..sys'..)

      (fact "stop"
        (sut/stop-system! container :key) => container
        (provided
         (c/stop ..sys'..) => ..sys''..)
        (sut/system container :key) => ..sys''..)

      (fact "remove"
        (sut/remove-system! container :key) => container
        (sut/system container :key) => nil)

      (fact "stop container"
        (sut/add-system! container :key ..sys..) => container
        (c/stop container) => anything
        (provided
         ;; should stop user systems too
         (c/stop ..sys..) => ..sys'..)))

    (fact "load-system!"
      (fact "config map"
        (let [container (c/start (impl/new-default-container {}))]
          (sut/load-system! container {:a {:a :a}
                                       :b {:b :b}
                                       :c false ; ignore
                                       }
                            :main)
          =>
          container
          (provided
           (c/build-nested-system-map {:a {:a :a}
                                       :b {:b :b}
                                       :c false ; ignore
                                       }) => ..sys..)
          (sut/system container :main) => ..sys..)))))
