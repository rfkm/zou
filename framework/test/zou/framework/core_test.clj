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

    (fact "load-system"
      (fact "config map"
        (let [core (c/start (sut/map->Core {}))]
          (sut/load-system core {:a {:a :a}
                                 :b {:b :b}
                                 :c false ; ignore
                                 })
          =>
          core
          (provided
           (c/build-nested-system-map {:a {:a :a}
                                       :b {:b :b}
                                       :c false ; ignore
                                       }) => ..sys..)
          (sut/system core) => ..sys..))

      (fact "config path"
        (let [core (c/start (sut/map->Core {}))]
          (sut/load-system core "conf-path")
          =>
          core
          (provided
           (conf/read-config "conf-path") => {:a {:a :a}
                                              :b {:b :b}
                                              :c false}
           (c/build-nested-system-map {:a {:a :a}
                                       :b {:b :b}
                                       :c false}) => ..sys..)
          (sut/system core) => ..sys..)))))
