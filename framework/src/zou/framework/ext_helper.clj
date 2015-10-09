(ns zou.framework.ext-helper
  (:require [zou.util.namespace :as un]))

(defn- inject-ns*
  ([to-ns]
   (inject-ns* to-ns []))
  ([to-ns exclusions]
   `(un/inject-ns ~to-ns [init deinit])))

(defmacro inject-ns
  {:arglists '([to-ns] [to-ns exclusions])}
  [& args]
  (apply inject-ns* args))
