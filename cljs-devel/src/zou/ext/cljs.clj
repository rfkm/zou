(ns zou.ext.cljs
  (:require [zou.cljs.figwheel :as fig]
            [zou.framework.container :as container]
            [zou.framework.core :as core]
            [zou.framework.ext-helper :as ext]
            [zou.logging :as log])
  (:import zou.cljs.figwheel.Figwheel))

(defn- find-figwheel-system []
  (for [[_ c] (container/system (:container (core/bootstrap-system)))
        :when (and (instance? Figwheel c)
                   (:figwheel-system c))]
    (:figwheel-system (:figwheel-system c))))

(defn cljs-repl
  ([& ks]
   (if-let [fig (get-in (container/system (:container (core/bootstrap-system))) ks)]
     (fig/cljs-repl fig)
     (throw (Exception. "Can't find figwheel component"))))
  ([]
   (if-let [figs (seq (find-figwheel-system))]
     (do
       (when (> (count figs) 1)
         (log/warn "Found multiple instances of figwheel"))
       (fig/cljs-repl (first figs)))
     (throw (Exception. "Can't find figwheel component")))))

(defn init []
  (ext/inject-ns zou.framework.repl))
