(ns zou.component.internal.util
  (:require [zou.util.namespace :as un :include-macros true]))

(defn resolve-ctor [ctor]
  (cond
    #?@(:clj [(symbol? ctor) (un/resolve-var ctor)])

    (ifn? ctor) ctor

    :else
    (throw (ex-info #?(:clj "Constructor must be a function or resoluble symbol"
                       :cljs "Constructor must be a function")
                    {:ctor ctor}))))

(defn try-catch-all [try-fn catch-fn]
  (try
    (try-fn)
    (catch #?(:cljs :default :clj Throwable) e
      (catch-fn e))))
