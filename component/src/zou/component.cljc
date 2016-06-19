(ns zou.component
  (:require [clojure.set :as set]
            [com.stuartsierra.component :as component]
            [com.stuartsierra.dependency :as dep]
            [zou.component.internal.util :as cu :include-macros true]
            [zou.component.parser :as p]
            [zou.component.parser.scanner]
            [zou.component.proto-ext :as pe]
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
          #?(:clj (pe/apply-protocol-extensions :instantiated))
          (apply-dependencies parsed)
          (apply-weak-dependencies parsed)
          #?(:clj (pe/apply-protocol-extensions :injected))
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

#?(:clj
   ;; TODO: CLJS support?
   (do
     (defn- guess-ctor [klass]
       (->> (.split (.getName klass) "\\.")
            last
            (str "map->")
            symbol
            (ns-resolve *ns*)))

     (defn- extract-ctor [v]
       (if (var? v)
         v
         (or (and (class? v)
                  (guess-ctor v))
             (throw
              (ex-info
               (str
                "Couldn't find a consturctor. "
                "You can only specify a defrecord or defn expression.")
               {})))))

     (defn- extract-deps [prefix defrecord-decl]
       (let [params (when (= (name (first defrecord-decl)) "defrecord")
                      (nth defrecord-decl 2))]
         (when (vector? params)
           (->> params
                (map (fn [sym]
                       (let [dep (->> (meta sym)
                                      keys
                                      (filter #(or (= (name prefix) (namespace %))
                                                   (= prefix %))))]
                         (when (> (count dep) 1)
                           (throw (ex-info "Multiple dep keys were found" {:dep-keys dep})))
                         (when (seq dep)
                           (let [dep-key (first dep)
                                 dep-name (if (= prefix dep-key)
                                            (keyword sym)
                                            (keyword (name dep-key)))]
                             [(keyword sym) dep-name])))))
                (filter identity)
                (into {})))))

     (def ^:private +component-macro-props+
       #{:name :constructor
         :tags :dependencies :optionals :dependants
         :disabled})

     (defmacro component
       "A utility macro to attach component metadata to a constructor
  guessed from a defrecord/defn expression. Using the specified name,
  other components can refer the component defined by this macro as a
  dependency. If defrecord is wrapped with the macro, its constructor
  will be assumed to be map->RecordName. Also, by attaching metadata
  to a parameter of defrecord, you can specify which component will be
  injected.

  Examples:
    (component
      :dependencies {:dep :another-component}
      (defn new-my-component [{:keys [dep]}]
        ...))

    (component my-foo  ;; <- Naming the component :my-foo
      :dependencies {:dep :another-component}
      (defrecord Foo [dep]
        ...))

    (component
      (defrecord Foo [;; A component named foo will be injected to :foo
                      ^:dep foo

                      ;; A component named jdbc will be injected to :db
                      ^:dep/jdbc db

                      ;; A component named opt will be injected to :opt
                      ;; as an weak dependency
                      ^:dep? opt

                      ;; A component named jdbc will be injected to :opt2
                      ;; as an optional dependency
                      ^:dep?/jdbc opt2]))"
       {:arglists '([name? (prop-key prop-val) * defrecord-or-defn])
        :style/indent :defn}
       [& args]
       (let [[id & args] (if (symbol? (first args))
                           args
                           (cons nil args))
             kvs (apply hash-map (butlast args))
             kvs (if-let [unknown-keys (seq (remove +component-macro-props+ (keys kvs)))]
                   (throw (ex-info
                           (str "Unknown property key(s): " unknown-keys)
                           {:unknown-keys unknown-keys}))
                   (u/filter-keys +component-macro-props+ (apply hash-map (butlast args))))
             kvs (if id (assoc kvs :name (keyword (name id))) kvs)
             cmp (last args)
             kvs (update kvs :dependencies merge (extract-deps :dep cmp))
             kvs (update kvs :optionals merge (extract-deps :dep? cmp))
             meta-map (u/map-keys #(keyword "zou" (name %)) kvs)]
         `(let [cmp# ~cmp
                ctor# (#'extract-ctor cmp#)]
            (alter-meta! ctor# assoc :zou/component ~meta-map)
            cmp#)))))
