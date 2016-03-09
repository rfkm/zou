(ns zou.framework.container
  (:require [zou.framework.container.proto :as proto]
            [zou.component :as c]))

(defn component [container component-key]
  (proto/get-component container component-key))

(defn system
  ([container]
   (proto/as-system container)))

(defn subsystem [container & ks]
  (proto/narrow container ks))

(defn start-system! [container]
  (proto/start-system container))

(defn stop-system! [container]
  (proto/stop-system container))
