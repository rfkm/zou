(ns zou.migrator.ragtime
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [inflections.core :as inf]
            [ragtime.core :as ragtime]
            [ragtime.jdbc :as jdbc]
            [zou.component :as c]
            [zou.logging :as log]
            [zou.task :as task])
  (:import java.time.format.DateTimeFormatter
           java.time.LocalDateTime))

(defn validate-component-state [component]
  (let [{:keys [datastore index migrations]} component]
    (when-not (and datastore index migrations)
      (throw (ex-info "Ragtime component has not been initialized" {:keys [:datastore :index :migrations]})))
    component))

(defn migrate [ragtime-component]
  (let [{:keys [datastore index migrations]} (validate-component-state ragtime-component)
        applied (ragtime/applied-migrations datastore index)
        opts (select-keys ragtime-component [:strategy :reporter])]
    (if (> (count index) (count applied))
      (do (ragtime/migrate-all datastore index migrations opts)
          (log/info "Successfully migrated"))
      (log/info "No migrations to apply"))))

(defn rollback [ragtime-component options]
  (let [{:keys [datastore index]} (validate-component-state ragtime-component)
        applied (ragtime/applied-migrations datastore index)
        {:keys [n id]} options
        n (if (nil? n) 1 n)
        opts (select-keys ragtime-component [:reporter])]
    (if (and (seq applied)
             (or (seq id) (integer? n)))
      (do
        (if (seq id)
          (ragtime/rollback-to datastore index id opts)
          (ragtime/rollback-last datastore index n opts))
        (log/info "Successfully rolled back"))
      (log/info "No migrations to roll back"))))

(defn- gen-file-name [description]
  (let [f (DateTimeFormatter/ofPattern "yyyyMMddHHmmssSSS")
        d (LocalDateTime/now)]
    (->> (str/replace description #"\s" "_")
         inf/underscore
         (str "v_" (.format d f) "_"))))

(defmulti generate-files (fn [style path description] style))

(defmethod generate-files :default
  [style _ _]
  (throw (ex-info "Invalid parameter" {:style style})))

(defmethod generate-files :sql
  [_ path description]
  (let [prefix (gen-file-name description)
        up-sql (io/file path (str prefix ".up.sql"))
        down-sql (io/file path (str prefix ".down.sql"))]
    (spit up-sql "")
    (spit down-sql "")
    [up-sql down-sql]))

(defmethod generate-files :edn
  [_ path description]
  (let [prefix (gen-file-name description)
        edn (io/file path (str prefix ".edn"))]
    (spit edn (str "{:up []" (System/lineSeparator) " :down []}"))
    [edn]))

(defn generate* [migrator description]
  (let [{:keys [migrations-path style]} migrator
        resource (io/resource migrations-path)]
    (if (and resource (= (.getProtocol resource) "file"))
      (generate-files style (-> resource io/file .getPath) description)
      (throw (ex-info (format "Migrations directory `%s` %s"
                              migrations-path
                              (if resource "not a directory" "does not exist"))
                      {:migrations-path migrations-path})))))

(defn generate [ragtime-component description]
  (doseq [file-name (map #(.getName %) (-> (validate-component-state ragtime-component)
                                           (generate* description)))]
    (log/info "Generate" file-name)))

(defrecord Ragtime [db]
  c/Lifecycle
  (start [this]
    (let [{:keys [migrations-path]} this
          migrations (jdbc/load-resources migrations-path)]
      (assoc this
             :migrations migrations
             :datastore (->> (select-keys this [:migrations-table])
                             (jdbc/sql-database db))
             :index (ragtime/into-index migrations))))
  (stop [this]
    (dissoc this :migrations :datastore :index))

  task/Task
  (task-name [this] :ragtime)
  (spec [this] {:desc "Ragtime tasks"})

  task/TaskContainer
  (tasks [this]
    [(task/task :migrate
                (fn [_]
                  (migrate this))
                :desc "Runs migration")

     (task/task :rollback
                (fn [{:keys [options]}]
                  (rollback this options))
                :desc "Rolls back the database"
                :option-specs [["-n" "--n N" "How many changes to rollback"
                                :parse-fn #(Long/parseLong %)
                                :validate [pos? "Must be integer"]]
                               ["-i" "--id ID" "Which id to rollback to"
                                :validate [seq "Must be non-empty string"]]])

     (task/task :generate
                (fn [env]
                  (generate this (get-in env [:arguments :description])))
                :desc "Generate migration files"
                :argument-specs [["description"
                                  :validate [seq "Must be non-empty string"]]])]))

(defn new-ragtime [conf]
  (map->Ragtime conf))
