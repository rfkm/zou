(ns zou.component
  (:require [clojure.set :as set]
            [com.stuartsierra.component :as component]
            [com.stuartsierra.dependency :as dep]
            [zou.component.internal.util :as cu :include-macros true]
            [zou.component.parser :as p]
            [zou.logging :as log :include-macros true]
            [zou.util :as u :include-macros true]
            [zou.util.namespace :as un :include-macros true]))

#?(:clj (un/import-ns com.stuartsierra.component #{dependency-graph})
   :cljs (un/cljs-import-ns com.stuartsierra.component #{dependency-graph}))

(defn- instantiate-components [parsed-config]
  (reduce-kv (fn [components component-key {:keys [constructor config disabled]
                                            :or   {constructor identity
                                                   config      {}
                                                   disabled    false}}]
               (if disabled
                 components
                 (assoc components component-key (constructor config))))
             {}
             (:components parsed-config)))

(defn- instantiate-system [parsed-config components]
  (let [ctor (cu/resolve-ctor (or (get-in parsed-config [:system :constructor])
                                  component/map->SystemMap))]
    (if (ifn? ctor)
      (ctor components)
      (throw (ex-info "System constructor is not callable"
                      {:constructor ctor})))))

(defn- dependency-map [parsed-config]
  (->> (:components parsed-config)
       (u/map-vals :dependencies)
       (u/filter-vals identity)))

(defn- weak-dependency-map [parsed-config]
  (->> (:components parsed-config)
       (u/map-vals :weak-dependencies)
       (u/filter-vals identity)))

(defn- apply-dependencies [parsed-config system]
  (component/system-using system (dependency-map parsed-config)))

(defn- apply-weak-dependencies [parsed-config system]
  (->> (weak-dependency-map parsed-config)
       (u/map-vals #(u/filter-vals (set (keys system)) %))
       (component/system-using system)))

(defn- dependency-keys [graph k]
  (filter #(dep/depends? graph k %) (dep/nodes graph)))

(defn- narrow-system [system subsystem-keys]
  (let [graph (component/dependency-graph system (keys system))
        ks (into subsystem-keys
                 (mapcat #(dependency-keys graph %) subsystem-keys))
        ks-to-del (set/difference (set (keys system)) (set ks))]
    (reduce (fn [system k] (dissoc system k)) system ks-to-del)))

(defn build-system-map
  "Build a system from the given configuration map. If
  `subsystem-keys` is given, the system will be narrowed to a system
  that consists of components corresponding to the keys including
  their dependencies."
  ([conf] (build-system-map conf ::all))
  ([conf subsystem-keys]
   (let [parsed (p/parse-system-config conf)]
     (->> (instantiate-components parsed)
          (instantiate-system parsed)
          (apply-dependencies parsed)
          (apply-weak-dependencies parsed)
          (u/?>> (not= subsystem-keys ::all)
                 (u/<- (narrow-system subsystem-keys)))))))

(defmacro try-recovery
  "Evaluates `body`, that typically contains an expression that starts
  a system, in a try expression. If an exception is thrown while
  evaluating the expression, this tries to recover the system by
  stopping it. There is no guarantee that the recovery phase will be
  successfully done, and it depends on implementations of the
  system (= components it has). To make your system safer, you should
  ensure each life cycle method is idempotence."
  [& body]
  `(cu/try-catch-all
    (fn [] ~@body)
    (fn [e#]
      (log/warn "Failed to start system")
      (log/warn "Trying to gracefully stop corrupted system...")
      (cu/try-catch-all
       (fn []
         (stop (:system (ex-data e#)))
         (log/warn "...succeeded"))
       (fn [e'#]
         (log/warn (ex-without-components e'#) "...failed")))
      (throw (ex-without-components e#)))))

(defmacro with-component
  "Evaluates `body` with `sym` bound to the started `component`, and a
  finally caluse that stops the `component`."
  [[sym component] & body]
  `(let [~sym (try-recovery (start ~component))]
     (try
       ~@body
       (finally
         (stop ~sym)))))

(defmacro with-system
  "Evaluates `body` with the `sym` bound to the started system created
  from the given config map, and a finally clause that stops the
  system. If `subsystem-keys` are given, the system will be narrowed
  down to a subsystem that contains components corresponding to the
  specified keys and their transitive dependencies."
  [[sym conf & subsystem-keys] & body]
  `(with-component [~sym (if (seq ~(vec subsystem-keys))
                           (build-system-map ~conf ~(vec subsystem-keys))
                           (build-system-map ~conf))]
     ~@body))
