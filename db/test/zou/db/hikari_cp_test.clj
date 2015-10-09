(ns zou.db.hikari-cp-test
  (:require [clojure.test :as t]
            [midje.sweet :refer :all]
            [zou.component :as c]
            [zou.db.hikari-cp :as sut]))

(t/deftest hikari-cp-component-test
  (fact
    (c/with-component [c (sut/map->HikariCP {:adapter "h2"
                                             :url "jdbc:h2:mem:test"})]
      (:datasource c)) => #(and (instance? javax.sql.DataSource %)
                                (.isClosed %))))
