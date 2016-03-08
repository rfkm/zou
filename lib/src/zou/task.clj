(ns zou.task
  (:require [cling.context :as ctx]
            [cling.entrypoint :as ce]
            [cling.util.project :as up]))

(defprotocol Task
  (task-name [this])
  (exec [this env])
  (spec [this]))

(defn task->cmd [task]
  (ctx/with-context
    (fn [env] (exec task env))
    (spec task)))

(defn tasks->container [tasks]
  (into {} (map (juxt task-name task->cmd) tasks)))

(defmacro generate-context []
  `(ce/gen-cli-options ~(up/guess-project-id)))

;;; TODO: move to cling?
(defn create-entrypoint [spec config]
  (let [ctx  (merge (generate-context) config)
        spec (ctx/with-context spec (ctx/merge-context ctx (ctx/get-context spec)))
        ep   (ce/create-handler spec ctx)]
    (fn [& args]
      (ep args))))
