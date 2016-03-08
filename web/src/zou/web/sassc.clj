(ns zou.web.sassc
  (:refer-clojure :exclude [compile])
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [clojure.string :as str]
            [hawk.core :as hawk]
            [schema.core :as s]
            [zou.component :as c]
            [zou.logging :as log]
            [zou.task :as task]
            [zou.web.asset.proto :as aproto]))

(def SasscConfig {:src s/Str
                  :output-to s/Str
                  (s/optional-key :sassc-cmd) s/Str
                  (s/optional-key :style) (s/enum "nested" "expanded" "compact" "compressed")
                  (s/optional-key :load-path) (s/either s/Str [s/Str])
                  (s/optional-key :sourcemap) s/Bool
                  (s/optional-key :precision) (s/both s/Int (s/pred pos?))
                  (s/optional-key :omit-map-comment) s/Bool
                  (s/optional-key :serve-path) s/Str}) ; for asset provider

(defn- vectorize [v]
  (if-not (vector? v)
    (if v (vector v) [])
    v))

(defn- opt-with-arg [k v]
  (if v
    [(str "--" (name k)) v]
    []))

(defn- opt [k v]
  (if v
    [(str "--" (name k))]
    []))

(defn- gen-args [conf]
  (map str
       (concat
        (mapcat #(opt % (% conf)) [:sourcemap :omit-map-comment])
        (mapcat #(opt-with-arg % (% conf)) [:style :precision])
        (mapcat #(mapcat (fn [v] (opt-with-arg % v))
                         (vectorize (% conf)))
                [:load-path])
        [(:src conf) (:output-to conf)])))

(s/defn ^:always-validate compile [conf :- SasscConfig]
  (io/make-parents (:output-to conf))
  (try
    (let [ret (apply sh/sh (cons (or (:sassc-cmd conf) "sassc") (gen-args conf)))]
      (if (= (:exit ret) 0)
        (log/infof "Successfully compiled %s to %s" (:src conf) (:output-to conf))
        (log/error "Failed to compile"))
      (when (seq (:err ret)) (log/errorn (:err ret)))
      (when (seq (:out ret)) (log/infon (:out ret))))
    (catch java.io.IOException e
      (log/error e "Did you properly install SassC?"))))

(s/defn ^:always-validate clean [conf :- SasscConfig]
  (io/delete-file (:output-to conf) true))

(defn- parent-dir [src]
  (some-> src
          io/file
          .getParentFile
          .getPath))

(defn- paths-to-watch [conf]
  (distinct
   (concat [(or (parent-dir (:src conf)) (:src conf))]
           (vectorize (:load-path conf)))))

(defn sass-file? [ctx e]
  (and (hawk/file? ctx e)
       (or (.endsWith (.getPath (:file e)) "scss")
           (.endsWith (.getPath (:file e)) "sass"))))

(s/defn touch [conf :- SasscConfig]
  (let [f (io/file (:output-to conf))]
    (when-not (.exists f)
      (io/make-parents f)
      (.createNewFile f))))

(s/defn watcher-spec [conf :- SasscConfig]
  {:paths (paths-to-watch conf)
   :filter sass-file?
   :handler (fn [ctx e]
              (touch conf)
              (compile conf))})

(defn- extract-asset-spec [conf]
  (when-let [p (:serve-path conf)]
    {:name p
     :type :stylesheet
     :src (:output-to conf)}))

(defrecord SasscCompiler [builds watch? initial-build?]
  c/Lifecycle
  (start [this]
    (s/validate [SasscConfig] builds)

    (when initial-build?
      (doseq [c builds]
        (touch c)
        (compile c)))

    (if (and (seq builds) watch?)
      (assoc this
             ::watcher (hawk/watch! (map watcher-spec builds)))
      this))
  (stop [this]
    (when-let [w (::watcher this)]
      (hawk/stop! w))
    (assoc this ::watcher nil))

  aproto/AssetsProvider
  (assets [this]
    (->> builds
         (map extract-asset-spec)
         (filter identity))))


;;; Task

(def task-spec (let [actions     #{:compile}
                     lit-actions (str/join ", " (map name actions))]
                 {:desc           "Run the Sass compiler"
                  :long-desc      (str "Run the Sass compiler. Available actions are: "
                                       lit-actions)
                  :argument-specs [["action"
                                    :parse-fn keyword
                                    :validate [actions (str "Currently supported actions are: "
                                                            lit-actions)]]]}))

(defmulti sassc-task (fn [action builds] action))

(defmethod sassc-task :default [action _]
  (log/errorf "Unknown action: %s" (name action)))

(defmethod sassc-task :compile [_ builds]
  (doseq [c builds]
    (touch c)
    (compile c)))

(defrecord SasscTask [builds]
  task/Task
  (task-name [this]
    :sassc)
  (spec [this]
    task-spec)
  (exec [this {:keys [arguments]}]
    (sassc-task (:action arguments) builds)))
