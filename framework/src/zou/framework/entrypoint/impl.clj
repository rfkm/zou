(ns zou.framework.entrypoint.impl
  (:require [cling.context :as ctx]
            [cling.response :as res]
            [schema.core :as s]
            [zou.component :as c]
            [zou.framework.cli :as cli]
            [zou.framework.container :as container]
            [zou.framework.container.proto :as cproto]
            [zou.framework.entrypoint.proto :as proto]
            [zou.framework.task :as task]))

(defn- default-cmd-fn [container]
  (ctx/with-context
    (fn [{:keys [arguments]}]
      (when (seq (:args arguments))
        (res/fail! (str "No such task: " (first (:args arguments)))))
      (container/start-systems! container)
      (res/keep-alive))
    {:argument-specs [["args" :variadic? true :optional? true]]}))

(defn- default-cmd-container [container]
  (ctx/with-context
    {true (default-cmd-fn container)}
    {:desc "Run the whole system"}))

(defn- find-tasks [container]
  (into {}
        (for [[sys-k sys]     (container/systems container)
              [k cmp] sys
              :when   (satisfies? task/Task cmp)]
          [[sys k] cmp])))

(defn- transitive-dep-keys [system k]
  (letfn [(f [k]
            (if-let [dep-keys (seq (vals (c/dependencies (get system k))))]
              (conj (mapcat f dep-keys) k)
              [k]))]
    (f k)))

(defn- narrow-down-system [system k]
  (->> (transitive-dep-keys system k)
       (select-keys system)
       c/map->SystemMap))

(defn- tasks->container [tasks]
  (into {}
        (for [[[sys k] t] tasks]
          [(task/task-name t)
           (ctx/with-context
             (fn [env]
               (c/with-component [s (narrow-down-system sys k)]
                 (task/exec (get s k) env)))
             (task/spec t))])))

(defn- create-entrypoint [container exit-process?]
  (->
   ;; Find all task components from container
   (find-tasks container)
   ;; Convert found task components into Cling's cmd container
   tasks->container
   ;; Add default handler
   (assoc true (default-cmd-container container))
   ;; Attach global option specs
   (ctx/with-context {:option-specs [cli/conf-option]})
   ;; Finally, create an entry point function
   (task/create-entrypoint {:exit-process? exit-process?})))


(s/defrecord DefaultEntryPoint [container :- (s/protocol cproto/SystemContainer)
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
