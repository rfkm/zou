(ns zou.web.middleware.reload
  (:require [clojure.java.io :as io]
            [ring.middleware.reload :as dep]
            [zou.logging :as log]
            [zou.util.namespace :as un])
  (:import java.io.FileNotFoundException))

(def reload-tag :zou/reload)

(defn wrap-reload-ns [handler {:keys [ns-finder-fn]
                               :or {ns-finder-fn all-ns}}]
  (fn [req]
    (doseq [ns (ns-finder-fn)]
      (log/debug "Reloading:" (ns-name ns))
      (try
        (require (ns-name ns) :reload)
        (catch FileNotFoundException _)))
    (handler req)))

(defn wrap-reload-tagged-var [handler]
  (wrap-reload-ns handler
                  {:ns-finder-fn
                   #(un/find-ns-contains-tagged-var [reload-tag])}))

(defn wrap-reload-tagged-ns [handler]
  (wrap-reload-ns handler
                  {:ns-finder-fn
                   #(un/find-tagged-ns [reload-tag])}))

(defn wrap-reload-enlive [handler]
  (let [tracker (atom {})]
    (wrap-reload-ns
     handler
     {:ns-finder-fn
      (fn []
        (for [ns (un/find-tagged-ns [:net.cgrand.reload/deps])
              :let [deps (:net.cgrand.reload/deps (meta (the-ns ns)))]
              f deps
              :when (= (.getProtocol f) "file")
              :let [last-modified (.lastModified (io/as-file f))]
              :when (or (not (contains? @tracker f))
                        (< (get @tracker f) last-modified))]
          (do
            (swap! tracker assoc f last-modified)
            ns)))})))

(defn wrap-reload [handler & [options]]
  (-> handler
      (dep/wrap-reload options)
      ;; (wrap-reload-tagged-var)
      ;; (wrap-reload-enlive)
      (wrap-reload-tagged-ns)))
