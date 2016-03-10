(ns zou.task
  (:require [cling.context :as ctx]
            [cling.entrypoint :as ce]
            [cling.util.project :as up]))

(defprotocol Task
  (task-name [this])
  (exec [this env])
  (spec [this]))

(defprotocol TaskContainer
  (tasks [this]))

(declare task-container->cmd-container)

(defn task->cmd [task]
  (if (satisfies? TaskContainer task)
    (task-container->cmd-container task)
    (ctx/with-context
      (fn [env] (exec task env))
      (spec task))))

(defn tasks->cmd-container [tasks]
  (into {} (map (juxt task-name task->cmd) tasks)))

(defn task-container->cmd-container [t-container]
  (ctx/with-context
    (tasks->cmd-container (tasks t-container))
    (spec t-container)))

(defmacro generate-context []
  `(ce/gen-cli-options ~(up/guess-project-id)))

;;; TODO: move to cling?
(defn create-entrypoint [spec config]
  (let [ctx  (merge (generate-context) config)
        spec (ctx/with-context spec (ctx/merge-context ctx (ctx/get-context spec)))
        ep   (ce/create-handler spec ctx)]
    (fn [& args]
      (ep args))))

(defn task [name handler & {:as spec-kvs}]
  (reify Task
    (task-name [this]
      name)
    (exec [this env]
      (handler env))
    (spec [this]
      spec-kvs)))

(defn task-container [name tasks & {:as spec-kvs}]
  (reify Task
    (task-name [this]
      name)
    (spec [this]
      spec-kvs)
    TaskContainer
    (tasks [this]
      tasks)))
