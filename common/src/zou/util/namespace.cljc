(ns zou.util.namespace
  (:require #?@(:clj
                [[bultitude.core :as b]
                 [clojure.tools.logging :as log]
                 [clojure.java.io :as io]
                 [hara.namespace.eval :as ne]
                 [potemkin.namespaces :as pn]])))

(defmacro cljs-import-vars [& syms]
  (let [unravel (fn unravel [x]
                  (if (sequential? x)
                    (->> x
                         rest
                         (mapcat unravel)
                         (map
                          #(symbol
                            (str (first x)
                                 (when-let [n (namespace %)]
                                   (str "." n)))
                            (name %))))
                    [x]))
        syms (mapcat unravel syms)]
    `(do
       ~@(map
          (fn [sym]
            `(def ~(symbol (name sym)) ~sym))
          syms))))

(defmacro import-vars [& syms]
  `(pn/import-vars ~@syms))

#?(:clj
   (defmacro cljs-import-ns*
     ([ns exclusions]
      (require 'cljs.analyzer.api)
      `(cljs-import-vars ~(into [ns] (->> ((ns-resolve 'cljs.analyzer.api 'ns-publics) ns)
                                          (remove (comp :macro second))
                                          keys
                                          (remove (set exclusions))))))))

#?(:clj
   (defmacro import-ns* [ns exclusions]
     (require ns)
     `(do
        (require (quote ~ns))
        (import-vars ~(into [ns] (remove (set exclusions)
                                         (keys (ns-publics ns))))))))

(defmacro import-ns
  ([ns]
   `(import-ns ~ns #{}))
  ([ns exclusions]
   `(import-ns* ~ns ~exclusions)))

(defmacro cljs-import-ns
  ;; NB: The CLJS version doesn't automatically require namespaces the
  ;; vars to import belong to, so you need to add them explicitly to
  ;; the ns declaration. Also, it doesn't import macros.
  ([ns]
   `(cljs-import-ns ~ns #{}))
  ([ns exclusions]
   `(cljs-import-ns* ~ns ~exclusions)))


;;; Clj only stuff

#?(:clj
   (do
     (defn resolve-var [sym]
       (let [ns (namespace (symbol sym))
             ns (when ns (symbol ns))]
         (when ns
           (require ns))
         (or (ns-resolve (or ns *ns*)
                         (symbol (name sym)))
             (throw (RuntimeException.
                     (format "Unable to resolve var: %s in this context" sym))))))

     (defn contains-tagged-var? [ns meta-key]
       (->> ns
            ns-interns
            vals
            (some #(get (meta %) meta-key))
            some?))

     (defn tagged-ns? [ns meta-key]
       (-> ns
           the-ns
           meta
           (get meta-key)
           boolean))

     (defn find-ns-contains-tagged-var [meta-keys]
       (->> (all-ns)
            (filter #(some (partial contains-tagged-var? %) meta-keys))
            (map ns-name)))

     (defn find-tagged-ns [meta-keys]
       (->> (all-ns)
            (filter #(some (partial tagged-ns? %) meta-keys))
            (map ns-name)))

     (defn find-tagged-vars
       ([meta-key]
        (find-tagged-vars meta-key identity))
       ([meta-key ns-filter]
        (->> (all-ns)
             (filter ns-filter)
             (mapcat #(vals (ns-interns %)))
             (filter #(get (meta %) meta-key)))))

     (defn safe-resolve-var [sym]
       (try
         (resolve-var sym)
         (catch Throwable _)))

     (defn- normalize-classpath [classpath]
       (cond
         (string? classpath)
         (.split classpath (System/getProperty "path.separator"))

         (coll? classpath)
         classpath

         :else
         (throw (ex-info "classpath` must be a collection or string" {:classpath classpath}))))

     (defn- classpath-files
       ([]
        (classpath-files []))
       ([exclude-classpath]
        (let [exclude-classpath (normalize-classpath exclude-classpath)
              fs                (set (map #(-> % io/file .getAbsolutePath) exclude-classpath))]
          (remove #(fs (.getAbsolutePath %)) (b/classpath-files)))))

     (defn require-all [exclude-classpath & prefixes]
       (doseq [prefix prefixes
               ns     (if exclude-classpath
                        (b/namespaces-on-classpath :prefix prefix :classpath (classpath-files exclude-classpath))
                        (b/namespaces-on-classpath :prefix prefix))]
         (log/debug "Loading:" ns)
         (require ns)))

     (defmacro inject [to-ns var-sym]
       (let [from-ns (ns-name *ns*)]
         `(do
            (try (require (quote ~to-ns))
                 (catch java.io.FileNotFoundException _#))
            (ne/with-ns (quote ~to-ns)
              (import-vars [~from-ns ~var-sym])))))

     (defmacro inject-ns [to-ns exclusions]
       (let [ns (ns-name *ns*)]
         `(do
            ~@(for [s (keys (ns-publics ns))
                    :when (not (contains? (set exclusions) s))]
                `(inject ~to-ns ~s)))))

     (defmacro with-temp-ns
       {:arglists '([[sym ns-name? forms & more] & body])}
       [bindings & body]
       (let [parse-bindings
             (fn parse-bindings [[sym ns-name forms & more :as bindings]]
               (when-not (symbol? sym)
                 (throw (ex-info "Unsupported binding form" {:rest bindings})))
               (if (string? ns-name)
                 (into [sym (symbol ns-name) forms] (when more (parse-bindings more)))
                 (into [sym (gensym "temp-ns") ns-name] (when more
                                                          (parse-bindings (cons forms more))))))
             ms (mapcat (fn [[sym ns-name forms]]
                          [sym
                           `(let [ns-name# (~'quote ~ns-name)]
                              (do (create-ns ns-name#)
                                  (ne/eval-ns ns-name# (~'quote ((clojure.core/refer-clojure))))
                                  (ne/eval-ns ns-name# ~forms)
                                  (the-ns ns-name#)))])
                        (partition 3 (parse-bindings bindings)))]
         `(let [~@ms]
            (try
              ~@body
              (finally
                ~@(for [ns (take-nth 2 ms)]
                    `(remove-ns (ns-name ~ns))))))))))
