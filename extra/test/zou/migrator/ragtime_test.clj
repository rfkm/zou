(ns zou.migrator.ragtime-test
  (:require [clojure.java.io :as io]
            [clojure.java.jdbc :as jdbc]
            [clojure.test :as t]
            [zou.component :as c]
            [zou.framework.bootstrap :as boot]
            [zou.framework.entrypoint.proto :as ep]
            [zou.logging :as log]
            [zou.migrator.ragtime :as sut])
  (:import java.nio.file.Files))

(def empty-file-attrs (into-array java.nio.file.attribute.FileAttribute []))

(def h2db-file (Files/createTempFile "h2db-" ".sql" empty-file-attrs))

(defn log [_ op id]
  (case op
    :up (log/info "Applying" id)
    :down (log/info "Rolling back" id)))

(def test-conf
  {:db (str "jdbc:h2:" (str h2db-file))
   :ragtime {:zou/constructor 'zou.migrator.ragtime/new-ragtime
             :zou/dependencies {:db :db}
             :migrations-path "migrations"
             :style :edn
             :strategy #'ragtime.strategy/raise-error
             :reporter #'log}})

(defn setup [migrations]
  (c/with-system [sys test-conf]
    (dotimes [n 3]
      (let [[edn] (sut/generate* (:ragtime sys) (str "migration_" n))]
        (spit edn (format "{:up [\"create table test_%s(id integer);\"] :down [\"drop table test_%s;\"]}" n n))
        (swap! migrations conj edn)))
    migrations))

(defn teardown [migrations]
  (c/with-system [sys test-conf]
    (jdbc/execute! (:db sys) "drop all objects;"))
  (doseq [edn @migrations]
    (io/delete-file edn)))

(reset-meta! *ns* {})
(t/use-fixtures :each
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

(t/deftest validate-component-state-test
  (t/testing "validate-component-state"
    (c/with-system [sys test-conf]
      (t/is (= (sut/validate-component-state (:ragtime sys))
               (:ragtime sys)))

      (t/is (thrown?
             clojure.lang.ExceptionInfo
             (= (-> (:ragtime sys)
                    (dissoc :index)
                    sut/validate-component-state)
                (:ragtime sys)))))))

(t/deftest migrate-test
  (t/testing "migrate"
    (c/with-system [sys test-conf]
      (t/is (= (find-test-tables (:db sys))
               #{}))

      (log/with-test-logger
        (sut/migrate (:ragtime sys))
        (doseq [o (map :msg (take 3 @log/*test-logger-entries*))]
          (t/is (re-matches #"^Applying v_\d{17}_migration_\d+$" o))))

      (t/is (= (find-test-tables (:db sys))
               #{"TEST_0" "TEST_1" "TEST_2"})))))

(t/deftest rollback-test
  (t/testing "rollback"
    (t/testing "with n option"
      (c/with-system [sys test-conf]
        (sut/migrate (:ragtime sys))
        (t/is (= (find-test-tables (:db sys))
                 #{"TEST_0" "TEST_1" "TEST_2"}))

        (log/with-test-logger
          (sut/rollback (:ragtime sys) {:n 2})
          (doseq [o (map :msg (take 2 @log/*test-logger-entries*))]
            (t/is (re-matches #"^Rolling back v_\d{17}_migration_\d+$" o))))

        (t/is (= (find-test-tables (:db sys))
                 #{"TEST_0"})))

      (c/with-system [sys test-conf]
        (sut/migrate (:ragtime sys))
        (t/is (= (find-test-tables (:db sys))
                 #{"TEST_0" "TEST_1" "TEST_2"}))

        (log/with-test-logger
          (sut/rollback (:ragtime sys) {:n nil})
          (log/logged? #"^Rolling back v_\d{17}_migration_\d+$"))

        (t/is (= (find-test-tables (:db sys))
                 #{"TEST_0" "TEST_1"}))))


    (t/testing "with id option"
      (c/with-system [sys test-conf]
        (sut/migrate (:ragtime sys))
        (t/is (= (find-test-tables (:db sys))
                 #{"TEST_0" "TEST_1" "TEST_2"}))

        (let [id (-> (jdbc/query (:db sys) "select * from ragtime_migrations order by created_at")
                     second
                     :id)]
          (log/with-test-logger
            (sut/rollback (:ragtime sys) {:id id})
            (doseq [o (map :msg (take 1 @log/*test-logger-entries*))]
              (t/is (re-matches #"^Rolling back v_\d{17}_migration_\d+$" o))))

          (t/is (= (find-test-tables (:db sys))
                   #{"TEST_0" "TEST_1"})))))))
