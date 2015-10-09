(ns zou.web.finder.rule
  (:require [zou.component :as c]
            [zou.util.namespace :as un]
            [zou.web.finder.proto :as proto]))

(defn expand-kw [kw mapping-conf]
  (let [root (get mapping-conf nil)
        tbl (dissoc mapping-conf nil)
        ns (namespace kw)
        act (name kw)]
    (or (and ns
             (->> tbl
                  (sort-by first #(compare (count (str %2))
                                           (count (str %1))))
                  (some (fn [[abbrev orig]]
                          (when (.startsWith ns (name abbrev))
                            (keyword (.replaceFirst ns (name abbrev) (name orig))
                                     act))))))
        (keyword (if ns
                   (str (and root
                             (str (name root) "."))
                        ns)
                   (name root))
                 act))))

(defn kw->var
  ([kw] (kw->var kw {}))
  ([kw mapping-conf]
   (let [expanded (expand-kw kw mapping-conf)]
     (un/resolve-var (symbol (namespace expanded) (name expanded))))))

(defrecord RuleBasedFinder [mappings]
  c/Lifecycle
  (start [this]
    (assoc this :finder (memoize #(kw->var % (or mappings {})))))
  (stop [this]
    (assoc this :finder nil))

  proto/Finder
  (find [this route-id]
    ((:finder this) route-id)))
