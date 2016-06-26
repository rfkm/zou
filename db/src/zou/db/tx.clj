(ns zou.db.tx
  "Provides a primary mechanism to handle transaction context as a
  monad. Note that the current implementation doesn't work nicely with
  several different types of transaction (e.g. multiple DB
  connections, other storage types, atom, etc.)."
  (:require [cats.context :as ctx]
            [cats.protocols :as p]))

(declare context)

(defprotocol Tx
  (-id [this]
    "Returns id to be used to discover the unit of work associated
    with this transaction. Any type of id is acceptable since it is
    just used as a key of a global context map.")
  (-atomic [this context func]
    ;; TODO: It might be better to split this method into two methods
    ;; such as commit and rollback so that we can handle several
    ;; different types of transaction within one global transaction.
    "Evaluates func as a transaction with the context the contents of
    which depend on each implementation. For example, the JDBC impl
    stores a connection, transaction mode, and current nested level of
    the transaction into the context. The context updated by this
    transaction (e.g., storing a connection newly opened, incrementing
    the nested level) is to be passed to the func."))

(defprotocol TxTask
  (-tx [this]
    "Returns Tx impl associated with this task.")
  (-run [this ctx]
    "Run a task."))

(defn- local-ctx [global-ctx tx]
  (get global-ctx (-id tx)))

(defn- assoc-local-ctx [global-ctx tx ctx]
  (assoc global-ctx (-id tx) ctx))

(defn- atomic* [tx global-ctx f]
  (-atomic tx
           (local-ctx global-ctx tx)
           (fn [ctx]
             (f (assoc-local-ctx global-ctx tx ctx)))))

(deftype Task [tx f]
  p/Contextual
  (-get-context [_] context)

  TxTask
  (-tx [this] tx)
  (-run [this ctx]
    (f ctx))

  clojure.lang.IFn
  (invoke [_]
    (atomic* tx {} f))
  (invoke [_ local-ctx]
    (atomic* tx (assoc-local-ctx {} tx local-ctx) f))

  clojure.lang.IDeref
  (deref [this]
    (atomic* tx {} f)))

(defn task [tx f]
  (Task. tx (fn [global-ctx]
              (f (local-ctx global-ctx tx)))))

(defn- task* [tx f]
  (Task. tx f))

(def identity-tx
  (reify Tx
    (-id [this] ::identity)
    (-atomic [this resource f]
      (f resource))))

(def context
  (reify
    p/Context
    (-get-level [_]
      ctx/+level-default+)

    p/Applicative
    (-pure [_ v]
      (task identity-tx
            (constantly v)))
    (-fapply [m af av]
      (task* (-tx af)
             (fn [ctx]
               ((-run af ctx) av))))

    p/Functor
    (-fmap [_ f fv]
      (task* (-tx fv)
             (fn [ctx]
               (f (-run fv ctx)))))

    p/Monad
    (-mreturn [_ v]
      (task identity-tx
            (constantly v)))
    (-mbind [_ mv f]
      (task* (-tx mv)
             (fn [ctx]
               (let [mv' (f (-run mv ctx))]
                 (atomic* (-tx mv')
                          ctx
                          (fn [ctx']
                            (-run mv' ctx')))))))))
