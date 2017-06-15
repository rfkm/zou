(ns zou.migrator.ragtime
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [inflections.core :as inf]
            [ragtime.core :as ragtime]
            [ragtime.jdbc :as jdbc]
            [zou.component :as c]
            [zou.logging :as log]
            [zou.task :as task])
  (:import java.nio.file.FileSystems
           java.time.format.DateTimeFormatter
           java.time.LocalDateTime))

(defprotocol IMigrator
  (migrate [this])
  (remigrate [this])
  (rollback [this opts])
  (generate [this description]))

(defn- gen-file-name [description]
  (let [f (DateTimeFormatter/ofPattern "yyyyMMddHHmmssSSS")
        d (LocalDateTime/now)]
    (->> (str/replace description #"\s" "_")
         inf/underscore
         (str "v_" (.format d f) "_"))))

(defn- gen-sql-files [path description]
  (let [separator (.getSeparator (FileSystems/getDefault))
        prefix (str path separator (gen-file-name description))
        up-sql (io/file (str prefix ".up.sql"))
        down-sql (io/file (str prefix ".down.sql"))]
    (spit up-sql "")
    (spit down-sql "")
    [up-sql down-sql]))

(defn- gen-edn-files [path description]
  (let [separator (.getSeparator (FileSystems/getDefault))
        prefix (str path separator (gen-file-name description))
        edn (io/file (str prefix ".edn"))]
    (spit edn (str "{:up []" (System/lineSeparator) " :down []}"))
    [edn]))

(defn generate* [migrator description]
  (let [{:keys [migrations-path style]} migrator
        path (some-> (io/resource migrations-path)
                     io/file
                     .getPath)]
    (when-not path
      (log/warnf "Migrations directory `%s` does not exist" migrations-path))
    (case style
      :edn (gen-edn-files path description)
      :sql (gen-sql-files path description)
      (throw (ex-info "Invalid parameter" {:style style})))))

(defrecord Ragtime [db]
  c/Lifecycle
  (start [this]
    (let [{:keys [migrations-path]} this
          migrations (jdbc/load-resources migrations-path)]
      (-> this
          (assoc :migrations migrations)
          (assoc :datastore (jdbc/sql-database db))
          (assoc :index (ragtime/into-index migrations)))))
  (stop [this]
    (dissoc this :migrations :datastore :index))

  IMigrator
  (migrate [this]
    (let [{:keys [datastore index migrations]} this
          applied (ragtime/applied-migrations datastore index)
          opts (select-keys this [:strategy :reporter])]
      (if (> (count index) (count applied))
        (do (ragtime/migrate-all datastore index migrations opts)
            (log/info "Successfully migrated"))
        (log/info "No migrations to apply"))))

  (remigrate [this]
    (let [{:keys [datastore index migrations]} this
          last-applied (last (ragtime/applied-migrations datastore index))
          opts (select-keys this [:reporter])]
      (if last-applied
        (do
          (ragtime/rollback-last datastore index 1 opts)
          (ragtime/migrate datastore last-applied)
          (log/info "Successfully remigrated"))
        (log/info "Applied migrations are nothing"))))

  (rollback [this options]
    (let [{:keys [datastore index]} this
          applied (ragtime/applied-migrations datastore index)
          {:keys [n id] :or {n 1}} options
          opts (select-keys this [:reporter])]
      (if (seq applied)
        (do
          (if (seq id)
            (ragtime/rollback-to datastore index id opts)
            (ragtime/rollback-last datastore index n opts))
          (log/info "Successfully rolled back"))
        (log/info "No migrations to roll back"))))

  (generate [this description]
    (doseq [file-name (map #(.getName %) (generate* this description))]
      (log/info "Generate" file-name)))

  task/Task
  (task-name [this] :migration)
  (spec [this] {:desc "Migration tasks"})

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
                                :validate [#(re-matches #"^\d+$" %) "Must be integer"]]
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
