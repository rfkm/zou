(ns zou.test-task
  (:require [zou.framework.task :as task]
            [zou.component :as c]))

(defrecord MyTask []
  c/Lifecycle
  (start [this]
    (println "Started MyTask")
    this)
  (stop [this]
    (println "Stopped MyTask")
    this)

  task/Task
  (task-name [this]
    :foo)
  (exec [this env]
    (println "foooo"))
  (spec [this]
    {:option-specs []
     :argument-specs []}))
