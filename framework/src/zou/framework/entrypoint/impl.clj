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
      (container/start-system! container)
      (res/keep-alive))
    {:argument-specs [["args" :variadic? true :optional? true]]}))

(defn- default-cmd-container [container]
  (ctx/with-context
    {true (default-cmd-fn container)}
    {:desc "Run the whole system"}))

(defn- find-tasks [container]
  (->> container
       container/system
       (filter (fn [[_ v]] (satisfies? task/Task v)))
       (into {})))

(defn- container-aware-cmd [container task-component-key]
  (let [system (container/system container)]
    (ctx/with-context
      (fn [env]
        (c/with-component [started-system (container/subsystem container task-component-key)]
          ;; refetch the task component from the started system
          (task/exec (get started-system task-component-key) env)))
      (task/spec (get system task-component-key)))))

(defn- assert-duplication [tasks]
  (let [task-names (map task/task-name tasks)]
    (when-not (apply distinct? task-names)
      (throw (ex-info "Found duplicated task name" {:task-names task-names})))))

(defn- container-aware-cmd-container [container t-container-key]
  (let [system (container/system container)
        t-container (get system t-container-key)
        tasks (task/tasks t-container)]
    (assert-duplication tasks)
    (ctx/with-context
      (into {}
            (for [t tasks
                  :let [t-name (task/task-name t)]]
              [t-name
               (ctx/with-context
                 (fn [env]
                   (c/with-component [started-system (container/subsystem container t-container-key)]
                     (task/exec (first (filter #(= t-name (task/task-name %))
                                               (task/tasks (get started-system t-container-key))))
                                env)))
                 (task/spec t))]))
      (task/spec t-container))))

(defn- tasks->cmd-container [container tasks]
  (assert-duplication (map last tasks))
  (into {}
        (for [[k t] tasks]
          [(task/task-name t)
           (if (satisfies? task/TaskContainer t)
             (container-aware-cmd-container container k)
             (container-aware-cmd container k))])))

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
