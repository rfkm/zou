(ns zou.web.finder.proto
  (:refer-clojure :exclude [find]))

(defprotocol Finder
  (find [this v]))

(extend-protocol Finder
  clojure.lang.APersistentMap
  (find [m v]
    (get m v))
  clojure.lang.Fn
  (find [f v]
    (find (f) v)))
