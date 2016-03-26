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
    env)
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

(defrecord ContainerTask []
  c/Lifecycle
  (start [this]
    (log/info "start container task")
    this)
  (stop [this]
    (log/info "stop container task")
    this)

  task/Task
  (task-name [this]
    :baz)
  (spec [this]
    {:option-specs [["-p" "--parent-opt"]]})

  task/TaskContainer
  (tasks [this]
    [(->FooTask)
     (->BarTask)]))

(t/deftest default-entry-point-test
  (facts "DefaultEntryPoint"
    (let [container (c/start (c.impl/new-stateful-container {:spec {:a {}
                                                                    :b {}
                                                                    :foo {:zou/constructor map->FooTask
                                                                          :zou/dependencies {:bar :bar}}
                                                                    :bar {:zou/constructor map->BarTask}
                                                                    :baz {:zou/constructor map->ContainerTask}}}))
          ep (c/start (sut/map->DefaultEntryPoint {:exit-process? false
                                                   :container container}))]
      (fact "If no subtask is specified, start the whole system."
        (proto/run ep []) => anything
        (provided
          ;; Ensure the default command is called
          (container/start-system! container) => ..started..))

      (fact "Run sub task"
        (log/with-test-logger
          (:options (proto/run ep ["foo" "-f"])) => (contains {:foo true})
          (log/logged? #"exec-foo") => true

          ;; Ensure task component's lifecycle works correctly
          (log/logged? #"start-foo") => true
          (log/logged? #"stop-foo") => true
          (log/logged? #"start-bar") => true
          (log/logged? #"stop-bar") => true)

        (get-in (proto/run ep ["foo"]) [:options :foo]) => nil
        (proto/run ep ["bar"]) => :bar-res
        (:options (proto/run ep ["baz" "foo" "-p"])) => (contains {:parent-opt true})))

    (fact "It's ok even if there is no task."
      (let [container (c/start (c.impl/new-stateful-container {:spec {:a {}}}))
            ep (c/start (sut/map->DefaultEntryPoint {:exit-process? false
                                                     :container container}))]
        (proto/run ep []) => anything))))
