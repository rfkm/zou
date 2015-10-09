(ns zou.component
  (:require [clojure.set :as set]
            [com.stuartsierra.component :as component]
            [com.stuartsierra.dependency :as dep]
            [zou.logging :as log]
            [zou.specter :as s]
            [zou.util :as u]
            [zou.util.namespace :as un]))

(un/import-ns com.stuartsierra.component)

(defn- system-tags [conf]
  (->> conf
       (s/select [s/ALL (s/collect-one s/FIRST) s/LAST :zou/tags s/ALL])
       (reduce (fn [acc [c t]]
                 (let [[t a] (if (vector? t) t [t c])]
                   (assoc-in acc [t a] c))) {})))

(defn- extract-ctor-def [conf-entry]
  (if-let [ctor (and (map? conf-entry)
                     (:zou/constructor conf-entry))]
    (if (symbol? ctor)
      (un/resolve-var ctor)
      ctor)
    ;; default ctor
    identity))

(defn- extract-deps [conf-entry]
  (:zou/dependencies conf-entry))

(defn- system-ctors [conf]
  (u/map-vals extract-ctor-def conf))

(defn- translate-tags [conf]
  (u/map-vals
   #(if (map? %) (dissoc % :zou/tags) %)
   (u/merge-nested
    conf
    (u/map-vals (partial hash-map :zou/dependencies)
                (system-tags conf)))))

(defn- translate-dependants [conf]
  (->> conf
       (s/select [s/ALL (s/collect-one s/FIRST) s/LAST :zou/dependants s/ALL])
       (reduce (fn [acc [comp-key [target alias]]]
                 (assoc-in acc [target :zou/dependencies alias] comp-key))
               conf)
       (s/transform [s/ALL s/LAST] #(if (map? %) (dissoc % :zou/dependants) %))))

(defn- translate-optionals [conf]
  (->> conf
       (s/transform [s/ALL s/LAST map?
                     (s/collect-one :zou/optionals) (s/view #(dissoc % :zou/optionals))
                     :zou/dependencies]
                    (fn [optionals deps]
                      (merge deps (or (u/filter-vals #(contains? conf %) optionals) {}))))))

(defn- system-deps [conf]
  (->> conf
       (u/map-vals extract-deps)
       (u/filter-vals identity)))

(defn- remove-special-keys [m]
  (if (map? m)
    (u/remove-keys #(and (keyword? %)
                         (= "zou" (namespace %)))
                   m)
    m))

(defn- system-confs [conf]
  (u/map-vals remove-special-keys conf))

(defn- cleanup-conf [conf]
  (->> conf
       ;; omit falsy entries
       (u/filter-vals identity)
       ;; omit disabled components
       (u/remove-vals (every-pred map? :zou/disabled))))

(def ^:private preprocess-conf (comp translate-optionals
                                     translate-dependants
                                     translate-tags
                                     cleanup-conf))

(defn- dependency-keys [graph k]
  (filter #(dep/depends? graph k %) (dep/nodes graph)))

(defn- dependency-graph [conf]
  (let [deps-map (system-deps conf)]
    (reduce-kv (fn [graph key deps]
                 (reduce #(dep/depend %1 key %2)
                         graph
                         (vals deps)))
               (dep/graph)
               deps-map)))

(defn- extract-subsystem-conf [conf ks]
  (let [conf (preprocess-conf conf)
        graph (dependency-graph conf)]
    (select-keys conf (into ks (mapcat #(dependency-keys graph %) ks)))))

(defn build-system-map
  ([conf] (build-system-map conf (keys conf)))
  ([conf subsystem-keys]
   (let [conf    (preprocess-conf conf)
         conf    (extract-subsystem-conf conf subsystem-keys)
         ctors   (system-ctors conf)
         confs   (system-confs conf)
         deps    (system-deps conf)
         sys-map (reduce (fn [acc [k ctor]]
                           (assoc acc k (ctor (get confs k))))
                         {}
                         ctors)]
     (-> (component/map->SystemMap sys-map)
         (component/system-using deps)))))

(defmacro try-recovery [& body]
  `(try
     ~@body
     (catch clojure.lang.ExceptionInfo e#
       (log/warn "Failed to start system")
       (log/warn "Trying to gracefully stop corrupted system...")
       (try (stop (:system (ex-data e#)))
            (log/warn "...succeeded")
            (catch Throwable e'#
              (log/warn (ex-without-components e'#) "...failed")))
       (throw (ex-without-components e#)))))

(defmacro with-component
  [[s component] & body]
  `(let [~s (try-recovery (start ~component))]
     (try
       ~@body
       (finally
         (stop ~s)))))

(defmacro with-system
  [[s conf & subsystem-keys] & body]
  `(with-component [~s (if (seq ~(vec subsystem-keys))
                         (build-system-map ~conf ~(vec subsystem-keys))
                         (build-system-map ~conf))]
     ~@body))
