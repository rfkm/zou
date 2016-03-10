(ns zou.task-test
  (:require [cling.context :as ctx]
            [midje.sweet :refer :all]
            [zou.task :as sut]))

(def auto-generated-ctx (sut/generate-context))

(def task-foo (sut/task :foo
                        (fn [env]
                          [:foo-exec env])))

(def task-bar (sut/task
               :bar
               (fn [env]
                 (get-in env [:options :bar]))
               :option-specs [["-b" "--bar"]]))

(def task-container (sut/task-container
                     :baz
                     [task-foo
                      task-bar]
                     :option-specs [["-p" "--parent"]]))

(facts "task->cmd"
  (fact "task"
    ((sut/task->cmd task-foo) ..env..) => [:foo-exec ..env..]
    (ctx/get-context (sut/task->cmd task-foo)) => (sut/spec task-foo))

  (fact "container"
    (sut/task->cmd task-container) => (just {:foo fn?
                                             :bar fn?})

    ((:foo (sut/task->cmd task-container)) ..env..) => [:foo-exec ..env..]
    (ctx/get-context (sut/task->cmd task-container)) => (sut/spec task-container)))

(fact "tasks->cmd-container"
  (sut/tasks->cmd-container [task-foo task-bar]) => {:foo ..foo-cmd..
                                                     :bar ..bar-cmd..}
  (provided
    (sut/task->cmd task-foo) => ..foo-cmd..
    (sut/task->cmd task-bar) => ..bar-cmd..))

(fact "create-entrypoint"
  ((sut/create-entrypoint (sut/task->cmd task-foo) {})) => (just [:foo-exec map?])
  ((sut/create-entrypoint (sut/task->cmd task-bar) {})) => nil
  ((sut/create-entrypoint (sut/task->cmd task-bar) {}) "-b") => true
  ((sut/create-entrypoint (sut/tasks->cmd-container [task-foo task-bar]) {}) "foo") => (just [:foo-exec map?])
  ((sut/create-entrypoint (sut/tasks->cmd-container [task-foo task-bar]) {}) "bar" "--bar") => true)
