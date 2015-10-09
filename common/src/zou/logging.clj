(ns zou.logging
  (:require [clojure.string :as str]
            [clojure.tools.logging]
            [clojure.tools.logging.impl :as impl]
            [io.aviso.logging :as pretty]
            [unilog.config :as unilog]
            [zou.util.namespace :as ns]))

(ns/import-ns clojure.tools.logging)

(defn- set-logback-dummy-file []
  (let [key "logback.configurationFile"]
    (when-not (System/getProperty key)
      (.. System
          (getProperties)
          (setProperty  key "zou/config/logback_dummy.xml")))))

(defn start-logging!
  ([]
   (start-logging! {:level :info :console true}))
  ([conf]
   (pretty/install-pretty-logging)
   (pretty/install-uncaught-exception-handler)

   ;; HACK: Prevent logback config set by unilog from being overwritten by wunderboss.
   (set-logback-dummy-file)
   (unilog/start-logging! conf)))

(defmacro logcol [level col-of-messages]
  `(doseq [m# ~col-of-messages]
     (logp ~level m#)))

(defmacro logn [level message]
  `(logcol ~level (str/split ~message #"\n")))

(defmacro tracen [message]
  `(logn :trace ~message))
(defmacro debugn [message]
  `(logn :debug ~message))

(defmacro infon [message]
  `(logn :info ~message))

(defmacro warnn [message]
  `(logn :warn ~message))

(defmacro errorn [message]
  `(logn :error ~message))

(defmacro fataln [message]
  `(logn :fatal ~message))

(defmacro tracecol [col-of-messages]
  `(logcol :trace ~col-of-messages))
(defmacro debugcol [col-of-messages]
  `(logcol :debug ~col-of-messages))

(defmacro infocol [col-of-messages]
  `(logcol :info ~col-of-messages))

(defmacro warncol [col-of-messages]
  `(logcol :warn ~col-of-messages))

(defmacro errorcol [col-of-messages]
  `(logcol :error ~col-of-messages))

(defmacro fatalcol [col-of-messages]
  `(logcol :fatal ~col-of-messages))


;;; test utils

(def ^:dynamic *test-logger-entries*)

(defn test-logger-factory [entries-atom]
  (reify impl/LoggerFactory
    (name [_] "test factory")
    (get-logger [_ log-ns]
      (reify impl/Logger
        (enabled? [_ level] true)
        (write! [_ level ex msg]
          (swap! entries-atom conj {:level level
                                    :ex ex
                                    :msg msg}))))))

(defmacro with-test-logger [& body]
  `(let [a# (atom [])]
     (binding [*test-logger-entries* a#
               clojure.tools.logging/*logger-factory* (test-logger-factory a#)]
       ~@body)))

(defn logged?
  ([] (logged? #".*" nil))
  ([msg] (logged? msg nil))
  ([msg level]
   (when-not (bound? #'*test-logger-entries*)
     (throw (Exception. "Didn't you call `logged?` from outside of `with-test-logger`?")))
   (boolean
    (some (fn [{msg' :msg level' :level}]
            (and (or (and (string? msg)
                          (= msg msg'))
                     (and (instance? java.util.regex.Pattern  msg)
                          (re-find msg msg')))
                 (or (nil? level)
                     (= level level'))))
          @*test-logger-entries*))))

(def ^{:arglists '([] [msg] [msg level])}
  not-logged?
  (complement logged?))
