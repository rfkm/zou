(ns zou.framework.task
  (:require [cling.context :as ctx]
            [cling.entrypoint :as ce]
            [cling.process :as proc]
            [cling.util.project :as up]))

(defmacro generate-context []
  `(ce/gen-cli-options ~(up/guess-project-id)))

;;; TODO: move to cling?
(defn create-entrypoint [spec config]
  (let [ctx  (merge (generate-context) config)
        spec (ctx/with-context spec (ctx/merge-context ctx (ctx/get-context spec)))
        ep   (ce/create-handler spec ctx)]
    (fn [& args]
      (binding [proc/*exit-process?* (:exit-process? config false)]
        (ep args)))))

(defprotocol Task
  (task-name [this])
  (exec [this env])
  (spec [this]))
