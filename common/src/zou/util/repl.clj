(ns zou.util.repl
  (:require [zou.util :as u]
            [zou.util.namespace :as un]))

(defonce ^:private out-bak (atom nil))

(defn create-system-out
  [out]
  (let [msg (un/safe-resolve-var 'clojure.tools.nrepl.middleware.interruptible-eval/*msg*)
        attach-id (fn [bindings]
                    (if-let [id (and msg (:id @msg))]
                      (assoc bindings msg {:id id})
                      bindings))
        bindings {#'*out* out}
        bindings (attach-id bindings)]
    (java.io.PrintStream.
     (proxy [java.io.ByteArrayOutputStream] []
       (flush []
         (let [^java.io.ByteArrayOutputStream this this]
           (proxy-super flush)
           (let [message (.trim (.toString this))]
             (proxy-super reset)
             (when (pos? (.length message))
               (with-bindings (attach-id bindings)
                 (println message)))))))
     true)))

(defn bridge-out! []
  (compare-and-set! out-bak nil [System/out System/err])
  (System/setOut  (create-system-out *out*))
  (System/setErr (create-system-out *err*)))

(defn restore-out! []
  (when-let [[out err] @out-bak]
    (swap! out-bak (constantly nil))
    (System/setOut out)
    (System/setErr err)))
