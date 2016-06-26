(ns zou.db.tx.jdbc
  "Provides utilities for JDBC transaction monad"
  (:refer-clojure :exclude [run!])
  (:require [clojure.java.jdbc :as jdbc]
            [zou.db.tx :as tx]
            [zou.util :as u]))

(defn ctx-transaction
  "Run a transaction with the given context. This is used mainly for
  implementing the `zou.db.tx/Tx` protocol."
  [ctx f]
  (u/mapply jdbc/db-transaction*
            (:db ctx)
            (fn [db]
              (f (assoc ctx :db db)))
            (:opts ctx {})))

(defn task
  "Similar to `zou.db.tx/task` but func directly takes a db the jdbc
  lib handles instead of a whole context map.

  Example:
    (def t (task tx-db
                 (fn [db]  ; <-- includes a transaction context
                   (first (jdbc/query db \"select * from users\")))))

    ;; To run the task, deref it
    (println @t) ;; => {:id 1 :name \"foo\"}

    ;; Or, you can use `run!` if you need change a transaction option
    (println (run! t {:read-only? true}))

    ;; You can use monadic operators since tasks are monads
    (println @(cats.core/fmap (fn [user] (:name user)) t))
    ;; => \"foo\"

    (defn find-status-by-user-id [tx-db uid]
      (task tx-db
            (fn [db]
              (jdbc/query
               db
               [\"select * from status where user_id = ?\" uid]))))
    (def t2 (cats.core/mlet [user t
                             status (find-status-by-user-id tx-db
                                                            (:id user))]
              (cats.core/return [user status])))
    (println @t2)
    ;; => [{:id 1 :name \"foo\"} {:id 1 :user_id 1 :status \"ok\"}]"
  [tx func]
  (tx/task tx (fn [ctx]
                (func (:db ctx)))))

(defn run!
  "Run the given task. This is not needed unless you need to pass
  options such as an isolation level. If you don't need options, it's
  easier to deref the task than calling this function. You can find a
  complete list of available options in the doc of
  `clojure.java.jdbc/db-transaction*`."
  ([t]
   (run! t {}))
  ([t opts]
   (t {:opts opts})))
