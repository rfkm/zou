(ns zou.framework.core-test
  (:require [clojure.test :as t]
            [midje.sweet :refer :all]
            [zou.component :as c]
            [zou.framework.config :as conf]
            [zou.framework.core :as sut]))

(t/deftest core-test
  (fact "core"
    (let [core (c/start (sut/map->Core {}))]
      ;; lifecycle test
      (fact "add"
        (sut/add-system core :key ..sys..) => core
        (sut/system core :key) => ..sys..)

      (fact "start"
        (sut/start-system core :key) => core
        (provided
          (c/start ..sys..) => ..sys'..)
        (sut/system core :key) => ..sys'..)

      (fact "stop"
        (sut/stop-system core :key) => core
        (provided
          (c/stop ..sys'..) => ..sys''..)
        (sut/system core :key) => ..sys''..)

      (fact "remove"
        (sut/remove-system core :key) => core
        (sut/system core :key) => nil)

      (fact "stop core"
        (sut/add-system core :key ..sys..) => core
        (c/stop core) => anything
        (provided
          ;; should stop subsystems too
          (c/stop ..sys..) => ..sys'..)))

    (fact "load-systems"
      (fact "config map"
        (let [core (c/start (sut/map->Core {}))]
          (sut/load-systems core {:a {:a :a}
                                  :b {:b :b}
                                  :c false ; ignore
                                  })
          =>
          core
          (provided
            (c/build-system-map {:a :a}) => ..sys-a..
            (c/build-system-map {:b :b}) => ..sys-b..)
          (sut/system core :a) => ..sys-a..
          (sut/system core :b) => ..sys-b..))

      (fact "config path"
        (let [core (c/start (sut/map->Core {}))]
          (sut/load-systems core "conf-path")
          =>
          core
          (provided
            (conf/read-config "conf-path") => {:a {:a :a}
                                               :b {:b :b}
                                               :c false}
            (c/build-system-map {:a :a}) => ..sys-a..
            (c/build-system-map {:b :b}) => ..sys-b..)
          (sut/system core :a) => ..sys-a..
          (sut/system core :b) => ..sys-b..)))))
