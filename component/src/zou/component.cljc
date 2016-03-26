(ns zou.component
  (:require [clojure.set :as set]
            [com.stuartsierra.component :as component]
            [com.stuartsierra.dependency :as dep]
            [zou.logging :as log :include-macros true]
            [zou.util :as u]
            [zou.util.namespace :as un :include-macros true]))

#?(:clj (un/import-ns com.stuartsierra.component #{dependency-graph})
   :cljs (un/cljs-import-ns com.stuartsierra.component #{dependency-graph}))

(defn- system-tags [conf]
  (->> (for [[k cmp] conf
             tag (:zou/tags cmp)]
         [k tag])
       (reduce (fn [acc [c t]]
                 (let [[t a] (if (vector? t)
                               t
                               [t
                                ;; Use the component name (without ns prefix) as an alias
                                (keyword (name c))])]
                   (assoc-in acc [t a] c))) {})))

(defn- extract-ctor-def [conf-entry]
  (when-let [ctor (and (map? conf-entry)
                       (:zou/constructor conf-entry))]
    (cond
      #?@(:clj [(symbol? ctor) (un/resolve-var ctor)])
      (fn? ctor) ctor
      :else (throw (ex-info #?(:clj "Constructor must be a function or resoluble symbol"
                               :cljs "Constructor must be a function")
                            {:ctor ctor})))))

(defn- extract-deps [conf-entry]
  (:zou/dependencies conf-entry))

(defn- system-ctors [conf]
  (u/map-vals (some-fn extract-ctor-def (constantly identity)) conf))

(defn- translate-tags [conf]
  (u/map-vals
   #(if (map? %) (dissoc % :zou/tags) %)
   (u/deep-merge
    conf
    (u/map-vals (partial hash-map :zou/dependencies)
                (system-tags conf)))))

(defn- translate-dependants [conf]
  (->> (for [[k cmp] conf
             d (:zou/dependants cmp)]
         [k d])
       (reduce (fn [acc [comp-key [target alias]]]
                 (assoc-in acc [target :zou/dependencies alias] comp-key))
               conf)
       (u/map-vals #(if (map? %) (dissoc % :zou/dependants) %))))

(defn- translate-optionals [conf]
  (u/map-vals (fn [cmp]
                (if (map? cmp)
                  (let [opts (:zou/optionals cmp)
                        deps (:zou/dependencies cmp)]
                    (-> cmp
                        (dissoc :zou/optionals)
                        (assoc :zou/dependencies
                               (merge deps
                                      (or (u/filter-vals #(contains? conf %) opts)
                                          {})))))
                  cmp)) conf))

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

(defn extract-subsystem-conf [conf ks]
  (let [conf  (preprocess-conf conf)
        graph (dependency-graph conf)]
    (select-keys conf (into ks (mapcat #(dependency-keys graph %) ks)))))

(defn build-system-map
  ([conf] (build-system-map conf (keys conf)))
  ([conf subsystem-keys]
   (let [ctor    (or (extract-ctor-def conf) component/map->SystemMap)
         conf    (dissoc conf :zou/constructor)
         conf    (preprocess-conf conf)
         conf    (extract-subsystem-conf conf subsystem-keys)
         ctors   (system-ctors conf)
         confs   (system-confs conf)
         deps    (system-deps conf)
         sys-map (reduce (fn [acc [k ctor]]
                           (assoc acc k (ctor (get confs k))))
                         {}
                         ctors)]
     (-> (ctor sys-map)
         (component/system-using deps)))))

(defn- munge-system-key [system-key]
  (if-let [ns (namespace system-key)]
    (keyword (str ns \. (name system-key)))
    system-key))

(defn- qualify-component-key [system-key component-key]
  (if (namespace component-key)
    component-key                       ; already qualified
    (keyword (name (munge-system-key system-key))
             (name component-key))))

(defn- qualify-component-spec [system-key spec]
  (let [qualify (partial qualify-component-key system-key)]
    (-> spec
        (u/update-in-when [:zou/dependencies] (partial u/map-vals qualify))
        (u/update-in-when [:zou/optionals] (partial u/map-vals qualify))
        (u/update-in-when [:zou/dependants] (partial u/map-keys qualify))
        (u/update-in-when [:zou/tags] (partial mapv #(cond
                                                       (keyword? %) (qualify %)
                                                       (vector? %)  (update % 0 qualify)
                                                       :else        %))))))

(defn flatten-nested-system-map [conf]
  {:pre [(and (map? conf) (every? map? (map last conf)))]}
  (into
   {}
   (for [[sys-key sys] conf
         [cmp-key cmp] sys]
     [(qualify-component-key sys-key cmp-key)
      (qualify-component-spec sys-key cmp)])))

(defn build-nested-system-map
  ([conf]
   (build-system-map (flatten-nested-system-map conf)))
  ([conf subsystem-keys]
   (build-system-map (flatten-nested-system-map conf) subsystem-keys)))

(defn try-catch-all [try-fn catch-fn]
  (try
    (try-fn)
    (catch #?(:cljs :default :clj Throwable) e
      (catch-fn e))))

(defmacro try-recovery [& body]
  `(try-catch-all
    (fn [] ~@body)
    (fn [e#]
      (log/warn "Failed to start system")
      (log/warn "Trying to gracefully stop corrupted system...")
      (try-catch-all
       (fn []
         (stop (:system (ex-data e#)))
         (log/warn "...succeeded"))
       (fn [e'#]
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

(defmacro with-nested-system
  [[s conf & subsystem-keys] & body]
  `(with-component [~s (if (seq ~(vec subsystem-keys))
                         (build-nested-system-map ~conf ~(vec subsystem-keys))
                         (build-nested-system-map ~conf))]
     ~@body))
