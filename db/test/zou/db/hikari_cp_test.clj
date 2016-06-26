(ns zou.db.hikari-cp-test
  (:require [cats.core :as m]
            [clojure.java.jdbc :as jdbc]
            [clojure.test :as t]
            [midje.sweet :refer :all]
            [zou.component :as c]
            [zou.db.hikari-cp :as sut]
            [zou.db.tx.jdbc :as tx]))

(t/deftest hikari-cp-component-test
  (fact
    (c/with-component [c (sut/map->HikariCP {:adapter "h2"
                                             :url "jdbc:h2:mem:test"})]
      (:datasource c)) => #(and (instance? javax.sql.DataSource %)
                                (.isClosed %))))

(t/deftest tx-test
  (c/with-component [db (sut/map->HikariCP {:adapter "h2"
                                            :url "jdbc:h2:mem:test"})]
    (jdbc/execute! db [(jdbc/create-table-ddl :user
                                              [:id :int "PRIMARY KEY"]
                                              [:name "varchar(255)"])])

    (try
      @(m/mlet [_ (tx/task db (fn [db]
                                (jdbc/insert! db :user {:id 1 :name "foo"})))
                _ (tx/task db (fn [db]
                                (jdbc/insert! db :user {:id 2 :name "bar"})))]
         (m/return :ok))
      (catch RuntimeException _))
    (t/is (= (jdbc/query db "select * from user")
             [{:id 1 :name "foo"}
              {:id 2 :name "bar"}]))))

(t/deftest tx-rollback-test
  (c/with-component [db (sut/map->HikariCP {:adapter "h2"
                                            :url "jdbc:h2:mem:test"})]
    (jdbc/execute! db [(jdbc/create-table-ddl :user
                                              [:id :int "PRIMARY KEY"]
                                              [:name "varchar(255)"])])
    (try
      @(m/mlet [_ (tx/task db (fn [db]
                                (jdbc/insert! db :user {:id 1 :name "foo"})))
                _ (tx/task db (fn [db]
                                (throw (RuntimeException. "error"))))
                _ (tx/task db (fn [db]
                                (jdbc/insert! db :user {:id 2 :name "bar"})))]
         (m/return :ok))
      (catch RuntimeException _))
    (t/is (= (seq (jdbc/query db "select * from user"))
             nil))))
