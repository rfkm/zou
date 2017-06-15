(ns zou.migrator.ragtime-test
  (:require [clojure.java.io :as io]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as str]
            [clojure.test :as t]
            [zou.component :as c]
            [zou.migrator.ragtime :as sut]))

(def h2db-dir "/tmp/h2-zou.migrator.ragtime")
(def h2db-file (str h2db-dir "/h2db.sql"))

(def test-conf
  {:db (str "jdbc:h2:" h2db-file)
   :ragtime {:zou/constructor 'zou.migrator.ragtime/new-ragtime
             :zou/dependencies {:db :db}
             :migrations-path "migrations"
             :style :edn
             :strategy #'ragtime.strategy/raise-error
             :reporter #'ragtime.reporter/print}})

(defn setup [migrations]
  (c/with-system [sys test-conf]
    (dotimes [n 3]
      (let [[edn] (sut/generate* (:ragtime sys) (str "migration_" n))]
        (spit edn (format "{:up [\"create table test_%s(id integer);\"] :down [\"drop table test_%s;\"]}" n n))
        (swap! migrations conj edn)))
    migrations))

(defn teardown [migrations]
  (letfn [(delete-recur [func f]
            (when (.isDirectory f)
              (doseq [f' (.listFiles f)]
                (func func f')))
            (io/delete-file f))]
    (delete-recur delete-recur (io/file h2db-dir)))
  (doseq [edn @migrations]
    (io/delete-file edn)))

(reset-meta! *ns* {})
(t/use-fixtures :once
  (fn [f]
    (let [migrations (atom [])]
      (setup migrations)
      (f)
      (teardown migrations))))

(defn find-test-tables [db]
  (->> (jdbc/query db "select * from information_schema.tables")
       (map :table_name)
       (filter #(re-matches #"^TEST_\d+$" %))
       (into #{})))

(t/deftest ragtime-component-test
  (t/testing "migrate"
    (c/with-system [sys test-conf]
      (t/is (= (find-test-tables (:db sys))
               #{}))
      (let [output (-> (with-out-str (sut/migrate (:ragtime sys)))
                       str/split-lines)]
        (doseq [o (take 3 output)]
          (t/is (re-matches #"^Applying v_\d{17}_migration_\d+$" o))))

      (t/is (= (find-test-tables (:db sys))
               #{"TEST_0" "TEST_1" "TEST_2"}))))

  (t/testing "rollback"
    (t/testing "with n option"
      (c/with-system [sys test-conf]
        (sut/migrate (:ragtime sys))
        (t/is (= (find-test-tables (:db sys))
                 #{"TEST_0" "TEST_1" "TEST_2"}))

        (let [output (-> (with-out-str (sut/rollback (:ragtime sys) {:n 2}))
                         str/split-lines)]
          (doseq [o (take 2 output)]
            (t/is (re-matches #"^Rolling back v_\d{17}_migration_\d+$" o))))

        (t/is (= (find-test-tables (:db sys))
                 #{"TEST_0"}))))


    (t/testing "with id option"
      (c/with-system [sys test-conf]
        (sut/migrate (:ragtime sys))
        (t/is (= (find-test-tables (:db sys))
                 #{"TEST_0" "TEST_1" "TEST_2"}))

        (let [id (-> (jdbc/query (:db sys) "select * from ragtime_migrations order by created_at")
                     second
                     :id)
              output (-> (with-out-str (sut/rollback (:ragtime sys) {:id id}))
                         str/split-lines)]
          (doseq [o (take 1 output)]
            (t/is (re-matches #"^Rolling back v_\d{17}_migration_\d+$" o))))

        (t/is (= (find-test-tables (:db sys))
                 #{"TEST_0" "TEST_1"}))))))
