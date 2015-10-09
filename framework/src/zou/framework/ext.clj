(ns zou.framework.ext
  (:require [bultitude.core :as b]
            [zou.logging :as log]))

(def ^:private ext-ns-prefix "zou.ext")
(def ^:private ext-init-fn 'init)
(def ^:private ext-deinit-fn 'deinit)

(defn extension-namespaces []
  (distinct (b/namespaces-on-classpath :prefix ext-ns-prefix)))

(defn extension-initializer [ext-ns]
  (try
    (require ext-ns)
    (ns-resolve ext-ns ext-init-fn)
    (catch java.io.FileNotFoundException _)))

(defn initialize-extension [ext-ns]
  (when-let [init (extension-initializer ext-ns)]
    (log/debug (str "Initializing extension: " ext-ns))
    (init)))

(defn initialize-extensions []
  (doseq [ns (extension-namespaces)]
    (initialize-extension ns)))

(defn extension-deinitializer [ext-ns]
  (try
    (require ext-ns)
    (ns-resolve ext-ns ext-deinit-fn)
    (catch java.io.FileNotFoundException _)))

(defn deinitialize-extension [ext-ns]
  (when-let [deinit (extension-deinitializer ext-ns)]
    (log/debug (str "Deinitializing extension: " ext-ns))
    (deinit)))

(defn deinitialize-extensions []
  (doseq [ns (extension-namespaces)]
    (deinitialize-extension ns)))
