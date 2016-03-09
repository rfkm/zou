(ns zou.framework.entrypoint.impl
  (:require [cling.context :as ctx]
            [cling.response :as res]
            [schema.core :as s]
            [zou.component :as c]
            [zou.framework.cli :as cli]
            [zou.framework.container :as container]
            [zou.framework.container.proto :as cproto]
            [zou.framework.entrypoint.proto :as proto]
            [zou.task :as task]))

(defn- default-cmd-fn [container]
  (ctx/with-context
    (fn [{:keys [arguments]}]
      (when (seq (:args arguments))
        (res/fail! (str "No such task: " (first (:args arguments)))))
      (container/start-system! container))
    {:argument-specs [["args" :variadic? true :optional? true]]}))

(defn- default-cmd-container [container]
  (ctx/with-context
    {true (default-cmd-fn container)}
    {:desc "Run the whole system"}))

(defn- find-tasks [container]
  (into {} (filter (fn [[k v]] (satisfies? task/Task v)) (container/system container))))

(defn- container-aware-cmd [container task-component-key]
  (let [system (container/system container)]
    (ctx/with-context
      (fn [env]
        (c/with-component [started-system (container/subsystem container task-component-key)]
          ;; refetch the task component from the started system
          (task/exec (get started-system task-component-key) env)))
      (task/spec (get system task-component-key)))))

(defn- tasks->cmd-container [container tasks]
  (into {}
        (for [[k t] tasks]
          [(task/task-name t)
           (container-aware-cmd container k)])))

(defn- create-entrypoint [container exit-process?]
  (->
   ;; Find all task components from container
   (find-tasks container)
   ;; Convert found task components into Cling's cmd container
   (->> (tasks->cmd-container container))
   ;; Add default handler
   (assoc true (default-cmd-container container))
   ;; Attach global option specs
   (ctx/with-context {:option-specs [cli/conf-option]})
   ;; Finally, create an entry point function
   (task/create-entrypoint {:exit-process? exit-process?})))


(s/defrecord DefaultEntryPoint [container :- (s/protocol cproto/ComponentContainer)
                                exit-process? :- s/Bool]
  {s/Keyword s/Any}
  c/Lifecycle
  (start [this]
    (s/validate DefaultEntryPoint this)
    this)
  (stop [this]
    this)

  proto/EntryPoint
  (run [this args]
    (let [ep (create-entrypoint container exit-process?)]
      (apply ep args))))
