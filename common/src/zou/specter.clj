(ns zou.specter
  (:require [com.rpl.specter]
            [zou.util.namespace :as ns]))

(ns/import-ns com.rpl.specter)


;;; Extensions

(defrecord AtomPath [])

(extend-protocol com.rpl.specter.protocols/StructurePath
  AtomPath
  (select* [this structure next-fn]
    (next-fn (deref structure)))
  (transform* [this structure next-fn]
    (swap! structure next-fn)
    structure))

(def atom-path (->AtomPath))
