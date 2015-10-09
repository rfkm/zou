(ns zou.web.middleware.out
  (:require [zou.util.namespace :as un]))

(defn wrap-nrepl-out-bridge [h]
  (if-let [msg (un/safe-resolve-var 'clojure.tools.nrepl.middleware.interruptible-eval/*msg*)]
    (let [id (:id @msg)
          out *out*
          err *err*]
      (fn [req]
        (with-bindings {msg {:id id}
                        #'*out* out
                        #'*err* err}
          (h req))))
    h))
