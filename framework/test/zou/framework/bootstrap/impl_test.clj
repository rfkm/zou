(ns zou.framework.bootstrap.impl-test
  (:require [clojure.test :as t]
            [midje.sweet :refer :all]
            [zou.component :as c]
            [zou.framework.bootstrap.impl :as sut]
            [zou.framework.entrypoint.proto :as ep]))

(t/deftest make-core-test
  (fact
    (type (sut/new-bootstrap-system ..conf..)) => zou.framework.bootstrap.impl.DefaultBootstrapSystem
    (provided
     (c/build-system-map ..conf..) => {})))

(t/deftest lifecycle-test
  (facts
    (fact
      (let [sys (sut/map->DefaultBootstrapSystem {:entrypoint (reify ep/EntryPoint)})]
        (c/start sys) => ..started..
        (provided
         (c/start-system sys) => ..started..)))

    (fact
      (let [sys (sut/map->DefaultBootstrapSystem {})]
        (c/stop sys) => ..stopped..
        (provided
         (c/stop-system sys) => ..stopped..)))

    (fact
      (let [sys (sut/map->DefaultBootstrapSystem {})]
        (c/start sys) => (throws clojure.lang.ExceptionInfo #"does not match schema")))))

(t/deftest run-test
  (fact
    (let [sys (sut/map->DefaultBootstrapSystem {:entrypoint (reify ep/EntryPoint
                                                              (run [this args]
                                                                args))})]
      (ep/run sys [:foo :bar]) => [:foo :bar])))
