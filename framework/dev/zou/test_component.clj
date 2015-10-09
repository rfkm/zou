(ns zou.test-component
  (:require [zou.component :as c]))

(defprotocol IRunner
  (run [this]))

(defrecord Counter [step]
  IRunner
  (run [this]
    (println (swap! (:count this) (partial + step))))

  c/Lifecycle
  (start [this]
    (println "Start counter component")
    (assoc this :count (atom 0)))
  (stop [this]
    (println "Stop counter component")
    (assoc this :count nil)))


(defrecord Timer [interval handlers]
  c/Lifecycle
  (start [this]
    (println "Start timer component")
    (let [fs (filter #(satisfies? IRunner %) (vals handlers))]
      (assoc this
             :threads (doall (for [f fs]
                               (future
                                 (loop []
                                   (run f)
                                   (Thread/sleep interval)
                                   (recur))))))))
  (stop [this]
    (println "Stop timer component")
    (run! future-cancel (:threads this))
    (assoc this :threads nil)))
