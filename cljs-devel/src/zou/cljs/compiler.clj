(ns zou.cljs.compiler
  (:require [cljs.build.api :as bapi]
            [zou.logging :as log]
            [zou.task :as task]
            [clojure.string :as str]
            [clojure.set :as set]
            [cling.core :as cli]))

(defn- normalize-build-conf [builds]
  (if (map? builds)
    (reduce-kv (fn [acc k v] (conj acc (assoc v :id k))) [] builds)
    builds))

(defn build-cljs* [build-conf]
  (let [t            (System/nanoTime)
        output-to    (get-in build-conf [:compiler :output-to])
        source-paths (:source-paths build-conf)]
    (try
      (log/infof "Compiling \"%s\" from %s..."
                 output-to
                 source-paths)
      (bapi/build
       (apply bapi/inputs source-paths)
       (:compiler build-conf))
      (log/infof "Successfully compiled \"%s\" in %.2fs."
                 output-to
                 (/ (- (System/nanoTime) t) 1e9))
      (catch Throwable e
        (log/errorf e "Failed to compile \"%s\" " output-to)))))

(defn build-cljs [build-confs]
  (doseq [build-conf build-confs]
    (build-cljs* build-conf)))

(defn- id->build-conf [id builds]
  {:pre [(not (nil? id))]}
  (let [ret (first (filter #(= (:id %) id) (normalize-build-conf builds)))]
    (when-not ret
      (throw (IllegalArgumentException. (str "No such build id: " id) )))
    ret))

(defrecord CLJSTasks [task-name builds]
  task/Task
  (task-name [this]
    (or task-name :cljs))
  (spec [this]
    {:desc "CLJS compiler tasks"})

  task/TaskContainer
  (tasks [this]
    [(task/task :compile
                (fn [{:keys [options]}]
                  (build-cljs
                   (if-let [ids (:build-ids options)]
                     (try
                       (doall (map #(id->build-conf % builds) ids))
                       (catch Throwable e
                         (cli/fail! (.getMessage e))))
                     (normalize-build-conf builds))))
                :desc "Compile CLJS"
                :option-specs [["-b" "--build-ids ID" "Build Ids to compile (comma separated)"
                                :parse-fn (fn [v] (map (comp keyword str/trim) (.split v ",")))]])]))
