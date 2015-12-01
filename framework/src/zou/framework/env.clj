(ns zou.framework.env
  (:require [environ.core :as env]))

(def env-key :zou-env)

(def hierarchy (-> (make-hierarchy)
                   (derive :test :dev)))

(defn- normalize-env [x]
  (some-> x name keyword))

(defn app-env
  "Returns name of current application environment as a keyword."
  []
  (normalize-env (env/env env-key :prod)))

(defn app-env-isa? [env]
  (isa? hierarchy (app-env) env))

(defmacro with-env
  "Temporarily redefines env while executing the body via with-redefs.
  This is NOT thread safe, so use this carefully."
  [m & body]
  `(with-redefs [env/env (merge env/env ~m)]
     ~@body))

(defmacro with-app-env [env & body]
  `(with-env {env-key ~env}
     ~@body))

(defmacro when-app-env [env & body]
  `(when (app-env-isa? ~env)
     ~@body))

(defmacro eval-when-app-env [env & body]
  `(when (app-env-isa? ~env)
     (eval (quote (do ~@body)))))


;;; ---
;;; generates env specific functions/macros

(defmacro ^:private defenv [env-name]
  `(do
     (defn ~(symbol (str "in-" (name env-name) "?")) []
       (app-env-isa? ~env-name))

     (defmacro ~(symbol (str "with-" (name env-name) "-env")) [& body#]
       (concat
        (list 'with-app-env ~env-name)
        body#))

     (defmacro ~(symbol (str "when-" (name env-name) "-env")) [& body#]
       (concat
        (list 'when-app-env ~env-name)
        body#))

     (defmacro ~(symbol (str "eval-when-" (name env-name) "-env")) [& body#]
       (concat
        (list 'eval-when-app-env ~env-name)
        body#))))

(defmacro ^:private defenvs [env-map]
  (let [defs (for [k env-map]
               `(defenv ~k))]
    `(do
       ~@defs)))

(defenvs [:prod :dev :test])
