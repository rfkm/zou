(ns zou.ext.{{namespace}}.repl
  (:require [zou.framework.ext-helper :as ext]))

;;; You can define repl functions here



;;; ========================================================
;;; Extension initializer/deinitializer

(defn init []
  (ext/inject-ns zou.framework.repl))

(defn deinit [])
