(ns zou.framework.entrypoint.impl-test
  (:require [clojure.test :as t]
            [midje.sweet :refer :all]
            [zou.component :as c]
            [zou.framework.container :as container]
            [zou.framework.container.impl :as c.impl]
            [zou.framework.entrypoint.impl :as sut]
            [zou.framework.entrypoint.proto :as proto]
            [zou.logging :as log]
            [zou.task :as task]))

(defrecord FooTask []
  c/Lifecycle
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
    {:option-specs [["-f" "--foo"]]}))

(defrecord BarTask []
  c/Lifecycle
  (start [this]
    (log/info "start-bar")
    this)
  (stop [this]
    (log/info "stop-bar")
    this)
  task/Task
  (task-name [this]
    :bar)
  (exec [this env]
    :bar-res)
  (spec [this]
    {}))

(t/deftest default-entry-point-test
  (facts "DefaultEntryPoint"
    (let [container (c/start (c.impl/new-default-container {:s {:a {}
                                                                :b {}
                                                                :foo {:zou/constructor map->FooTask
                                                                      :zou/dependencies {:bar :bar}}
                                                                :bar {:zou/constructor map->BarTask}}}))
          ep (c/start (sut/map->DefaultEntryPoint {:exit-process? false
                                                   :container container}))]
      (fact "If no subtask is specified, start the whole system."
        (proto/run ep []) => anything
        (provided
          ;; Ensure the default command is called
          (container/start-system! container) => ..started..))

      (fact "Run sub task"
        (log/with-test-logger
          (proto/run ep ["foo" "-f"]) => true
          (log/logged? #"exec-foo") => true

          ;; Ensure task component's lifecycle works correctly
          (log/logged? #"start-foo") => true
          (log/logged? #"stop-foo") => true
          (log/logged? #"start-bar") => true
          (log/logged? #"stop-bar") => true)

        (proto/run ep ["foo"]) => nil
        (proto/run ep ["bar"]) => :bar-res))))
