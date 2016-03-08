(ns zou.framework.entrypoint.impl-test
  (:require [clojure.test :as t]
            [midje.sweet :refer :all]
            [zou.component :as c]
            [zou.framework.container :as container]
            [zou.framework.entrypoint.impl :as sut]
            [zou.framework.entrypoint.proto :as proto]
            [zou.task :as task]
            [zou.logging :as log]))

(def task-foo (reify c/Lifecycle
                (start [this]
                  (log/info "start-foo")
                  this)
                (stop [this]
                  (log/info "stop-foo")
                  this)

                task/Task
                (task-name [this]
                  :foo)
                (exec [this env]
                  (log/info "exec-foo")
                  (get-in env [:options :foo]))
                (spec [this]
                  {:option-specs [["-f" "--foo"]]})))

(def task-bar (reify task/Task
                (task-name [this]
                  :bar)
                (exec [this env]
                  :bar-res)
                (spec [this]
                  {})))

(t/deftest default-entry-point-test
  (facts "DefaultEntryPoint"
    (against-background
     (container/systems ..container..) => {:sys-a (c/map->SystemMap {:a {}
                                                                     :b {}
                                                                     :foo task-foo})
                                           :sys-b (c/map->SystemMap {:a {}
                                                                     :b {}
                                                                     :bar task-bar})})

    (let [ep (sut/map->DefaultEntryPoint {:container ..container..})]
      (fact "If no subtask is specified, start the whole system."
        (proto/run ep []) => anything
        (provided
         ;; Ensure the default command is called
         (container/start-systems! ..container..) => ..started..))

      (fact "Run sub task"
        (proto/run ep ["foo" "-f"]) => true
        (proto/run ep ["foo"]) => nil
        (proto/run ep ["bar"]) => :bar-res))))
