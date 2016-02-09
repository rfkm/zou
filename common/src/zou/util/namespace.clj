(ns zou.util.namespace
  (:require [bultitude.core :as b]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [hara.namespace.eval :as ne]
            [potemkin.namespaces :as pn]))

(pn/import-vars [potemkin.namespaces import-vars])

(defn resolve-var [sym]
  (let [ns (namespace (symbol sym))
        ns (when ns (symbol ns))]
    (when ns
      (require ns))
    (or (ns-resolve (or ns *ns*)
                    (symbol (name sym)))
        (throw (RuntimeException.
                (format "Unable to resolve var: %s in this context" sym))))))

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

(defn- import-ns*
  ([ns]
   (import-ns* ns #{}))
  ([ns exclusions]
   (require ns)
   `(do
      (require (quote ~ns))
      (pn/import-vars ~(into [ns] (remove (set exclusions)
                                          (keys (ns-publics ns))))))))

(defmacro import-ns
  {:arglists '([ns] [ns exclusions])}
  [& args]
  (apply import-ns* args))

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
               `(remove-ns (ns-name ~ns))))))))
