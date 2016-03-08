(ns zou.framework.cli
  (:require [clojure.java.io :as io]
            [clojure.tools.cli :as cli]
            [zou.framework.config :as conf]
            [zou.logging :as log]))

(def conf-option
  ["-c" "--config PATH" "Config file"
   :parse-fn io/file
   :default (conf/fetch-config-or-abort)
   :validate [#(.exists ^java.io.File %)  "File does not exist"
              #(.isFile ^java.io.File %)  "Path is not a regular file"
              #(.canRead ^java.io.File %) "File is unreadable"]
   :default-desc (str "resource://" conf/default-config-path)])

(defn- unknown-option-error? [err]
  (some? (re-find #"Unknown option" err)))

(defn extract-config-file [args]
  (let [{:keys [errors options]} (cli/parse-opts args [conf-option])
        errors (remove unknown-option-error? errors)]
    (if (seq errors)
      (do
        (doseq [err errors]
          (log/error err))
        (throw (IllegalArgumentException. "Failed to load config file")))
      (:config options))))
