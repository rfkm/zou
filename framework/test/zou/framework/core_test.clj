(ns zou.framework.core-test
  (:require [clojure.test :as t]
            [midje.sweet :refer :all]
            [zou.component :as c]
            [zou.framework.core :as sut]
            [zou.framework.entrypoint.proto :as ep]))

(t/deftest make-core-test
  (fact
    (type (sut/make-core ..conf..)) => zou.framework.core.CoreSystem
    (provided
     (c/build-system-map ..conf..) => {})))

(t/deftest lifecycle-test
  (facts
    (fact
      (let [sys (sut/map->CoreSystem {:entrypoint (reify ep/EntryPoint)})]
        (c/start sys) => ..started..
        (provided
         (c/start-system sys) => ..started..)))

    (fact
      (let [sys (sut/map->CoreSystem {})]
        (c/stop sys) => ..stopped..
        (provided
         (c/stop-system sys) => ..stopped..)))

    (fact
      (let [sys (sut/map->CoreSystem {})]
        (c/start sys) => (throws clojure.lang.ExceptionInfo #"does not match schema")))))

(t/deftest run-core-test
  (fact
    (let [sys (sut/map->CoreSystem {:entrypoint (reify ep/EntryPoint
                                                  (run [this args]
                                                    args))})]
      (sut/run-core sys :foo :bar) => [:foo :bar])))

(t/deftest boot-shutdown-test
  (fact
    (def core-var nil)
    (prerequisites
     (sut/make-core ..conf..) => ..core..
     (c/start ..core..) => ..started-core..
     (c/stop ..started-core..) => ..stopped-core..
     (sut/make-core ..conf2..) => ..core2..
     (c/start ..core2..) => ..started-core2..
     (c/stop ..started-core2..) => ..stopped-core2..)
    core-var => nil
    (sut/boot-core! #'core-var ..conf..) => ..started-core..
    core-var => ..started-core..
    ;; The old instance bound to the var will be replaced with new one after stopping it
    (sut/boot-core! #'core-var ..conf2..) => ..started-core2..
    core-var => ..started-core2..

    (sut/shutdown-core! #'core-var) => nil
    core-var => nil))
