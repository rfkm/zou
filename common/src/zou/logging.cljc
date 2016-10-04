(ns zou.logging
  (:require [clojure.string :as str]
            [zou.util.platform :as up]
            #?@(:clj
                [[clojure.tools.logging :as log]
                 [clojure.tools.logging.impl :as impl]
                 [io.aviso.logging :as pretty]
                 [unilog.config :as unilog]])))

(defmacro log [level & args]
  `(up/if-cljs
    (case ~level
      :debug (.debug ~'js/console ~@args)
      :error (.error ~'js/console ~@args)
      :fatal (.error ~'js/console ~@args)
      :info  (.info ~'js/console ~@args)
      ;; In CLJS, just ignore args and print stack trace.
      :trace (.trace ~'js/console)
      :warn  (.warn ~'js/console ~@args)
      (.log ~'js/console ~@args))
    (log/logp ~level ~@args)))

(defmacro logf [level & args]
  `(up/if-cljs
    ;; The console API supports formatter by default (though not all envs).
    (log ~level ~@args)
    (log/logf ~level ~@args)))

(defmacro logcol [level col-of-messages]
  `(doseq [m# ~col-of-messages]
     (log ~level m#)))

(defmacro logn [level message]
  `(logcol ~level (str/split ~message #"\n")))

(defmacro debug [& args]
  `(log :debug ~@args))

(defmacro debugf [& args]
  `(logf :debug ~@args))

(defmacro debugcol [coll]
  `(logcol :debug ~coll))

(defmacro debugn [message]
  `(logn :debug ~message))

(defmacro error [& args]
  `(log :error ~@args))

(defmacro errorf [& args]
  `(logf :error ~@args))

(defmacro errorcol [coll]
  `(logcol :error ~coll))

(defmacro errorn [message]
  `(logn :error ~message))

(defmacro fatal [& args]
  `(log :fatal ~@args))

(defmacro fatalf [& args]
  `(logf :fatal ~@args))

(defmacro fatalcol [coll]
  `(logcol :fatal ~coll))

(defmacro fataln [message]
  `(logn :fatal ~message))

(defmacro info [& args]
  `(log :info ~@args))

(defmacro infof [& args]
  `(logf :info ~@args))

(defmacro infocol [coll]
  `(logcol :info ~coll))

(defmacro infon [message]
  `(logn :info ~message))

(defmacro spy
  ([x]
   `(spy :debug ~x))
  ([level x]
   `(up/if-cljs
     (let [ret# ~x]
       (log ~level ret#)
       ret#)
     (log/spy ~level ~x))))

(defmacro spyf
  ([fmt x]
   `(spyf :debug ~fmt ~x))
  ([level fmt x]
   `(up/if-cljs
     (let [ret# ~x]
       (logf ~level ~fmt ret#)
       ret#)
     (log/spyf ~level ~fmt ~x))))

(defmacro trace [& args]
  `(log :trace ~@args))

(defmacro tracef [& args]
  `(logf :trace ~@args))

(defmacro tracecol [coll]
  `(logcol :trace ~coll))

(defmacro tracen [message]
  `(logn :trace ~message))

(defmacro warn [& args]
  `(log :warn ~@args))

(defmacro warnf [& args]
  `(logf :warn ~@args))

(defmacro warncol [coll]
  `(logcol :warn ~coll))

(defmacro warnn [message]
  `(logn :warn ~message))


;;; Clojure specific stuff

#?(:clj
   (do
     (defn- set-logback-dummy-file []
       (let [key "logback.configurationFile"]
         (when-not (System/getProperty key)
           (.. System
               (getProperties)
               (setProperty  key "zou/config/logback_dummy.xml")))))

     (defn start-logging!
       ([]
        (start-logging! {:level :info :console true}))
       ([{:keys [pretty?] :as conf}]
        (when pretty?
          (pretty/install-pretty-logging)
          (pretty/install-uncaught-exception-handler))

        ;; HACK: Prevent logback config set by unilog from being overwritten by wunderboss.
        (set-logback-dummy-file)
        (unilog/start-logging! (dissoc conf :pretty?))))


     ;; test utils

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
       (complement logged?))))
