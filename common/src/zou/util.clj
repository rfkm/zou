(ns zou.util
  (:require [clojure.set :as set]
            [com.rpl.specter :as s]
            [medley.core]
            [plumbing.core]
            [zou.util.namespace :as ns]))

(ns/import-ns plumbing.core)
(ns/import-ns medley.core)

(defmacro with-arglists
  "Inherit arglists from the given var as follows:

  (with-arglists #'fetch (def fetch-one (comp first fetch)))"
  [src-var & body]
  `(let [v# (do ~@body)]
     (alter-meta! v# assoc :arglists (:arglists (meta ~src-var)))
     v#))

(defn deep-merge
  [& vals]
  (if (every? map? vals)
    (apply merge-with deep-merge vals)
    (last vals)))

(defn deep-merge-with
  [f & maps]
  (apply
   (fn m [& maps]
     (if (every? map? maps)
       (apply merge-with m maps)
       (apply f maps)))
   maps))

(defn crc32-hex [s]
  (let [c (java.util.zip.CRC32.)]
    (.update c (.getBytes (name s)))
    (format "%X" (.getValue c))))

(defn maybe-deref [ref]
  (if (or (instance? clojure.lang.IDeref ref)
          (instance? java.util.concurrent.Future ref))
    (deref ref)
    ref))
