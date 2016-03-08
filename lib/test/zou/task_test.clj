(ns zou.task-test
  (:require [cling.context :as ctx]
            [midje.sweet :refer :all]
            [zou.task :as sut]))

(def auto-generated-ctx (sut/generate-context))

(def task-foo (reify sut/Task
                (task-name [this]
                  :foo)
                (exec [this env]
                  [:foo-exec env])
                (spec [this]
                  {})))

(def task-bar (reify sut/Task
                (task-name [this]
                  :bar)
                (exec [this env]
                  (get-in env [:options :bar]))
                (spec [this]
                  {:option-specs [["-b" "--bar"]]})))

(fact "task->cmd"
  ((sut/task->cmd task-foo) ..env..) => [:foo-exec ..env..]
  (ctx/get-context (sut/task->cmd task-foo)) => (sut/spec task-foo))

(fact "tasks->container"
  (sut/tasks->container [task-foo task-bar]) => {:foo ..foo-cmd..
                                                 :bar ..bar-cmd..}
  (provided
   (sut/task->cmd task-foo) => ..foo-cmd..
   (sut/task->cmd task-bar) => ..bar-cmd..))

(fact "create-entrypoint"
  ((sut/create-entrypoint (sut/task->cmd task-foo) {})) => (just [:foo-exec map?])
  ((sut/create-entrypoint (sut/task->cmd task-bar) {})) => nil
  ((sut/create-entrypoint (sut/task->cmd task-bar) {}) "-b") => true
  ((sut/create-entrypoint (sut/tasks->container [task-foo task-bar]) {}) "foo") => (just [:foo-exec map?])
  ((sut/create-entrypoint (sut/tasks->container [task-foo task-bar]) {}) "bar" "--bar") => true)
