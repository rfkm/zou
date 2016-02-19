(ns zou.util.platform)

;;; Taken from:
;;; https://github.com/plumatic/schema/blob/master/src/clj/schema/macros.clj
(defn cljs-env?
  "Take the &env from a macro, and tell whether we are expanding into cljs."
  [env]
  (boolean (:ns env)))

(defmacro if-cljs
  "Return then if we are generating cljs code and else for Clojure code.
   https://groups.google.com/d/msg/clojurescript/iBY5HaQda4A/w1lAQi9_AwsJ"
  [then else]
  (if (cljs-env? &env) then else))

(defmacro if-clj [then else]
  (if-not (cljs-env? &env) then else))

(defmacro when-cljs [& then]
  (when (cljs-env? &env)
    `(do ~@then)))

(defmacro when-clj [& then]
  (when-not (cljs-env? &env)
    `(do ~@then)))
