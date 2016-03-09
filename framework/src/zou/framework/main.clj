(ns zou.framework.main
  (:gen-class)
  (:require [cling.process :as proc]
            [zou.framework.cli :as cli]
            [zou.framework.config :as conf]
            [zou.framework.core :as core]))

(defn -main
  [& args]
  (let [core (-> args
                 cli/extract-config-file
                 conf/read-config
                 core/boot!)]
    (binding [proc/*exit-process?* true]
      (apply core/run core args))))
