(ns zou.web.handler
  (:require [clojure.string :as str]
            [zou.util :as u]
            [zou.web.handler.args-mapper :as mapper]
            [zou.web.middleware.proto :as proto]))

(defmacro -defhandler
  [handler-tag ns-tag name & fdecl]
  (let [m {handler-tag
           (if (keyword? (first fdecl))
             (first fdecl)
             (keyword (clojure.core/name (ns-name *ns*))
                      (clojure.core/name name)))}
        fdecl (if (keyword? (first fdecl))
                (next fdecl)
                fdecl)
        m (if (string? (first fdecl))
            (assoc m :doc (first fdecl))
            m)
        fdecl (if (string? (first fdecl))
                (next fdecl)
                fdecl)
        m (if (map? (first fdecl))
            (conj m (first fdecl))
            m)
        fdecl (if (map? (first fdecl))
                (next fdecl)
                fdecl)
        [params & fdecl] (if (vector? (first fdecl))
                           fdecl
                           (throw (IllegalArgumentException. "Cannot find params")))
        spec (mapper/gen-destructuring-spec params)
        m (assoc m :arglists (list 'quote (list (:params spec))))
        m (conj (if (meta name) (meta name) {}) m)]
    `(let [mapper# (:fn (mapper/gen-destructuring-spec (quote ~params)))]
       (alter-meta! *ns* assoc ~ns-tag true)
       (def ~(with-meta name m)
         (vary-meta
          (fn ~(:params spec) ~@fdecl)
          assoc
          :zou/args-mapper
          mapper#)))))

(defmacro defhandler
  {:arglists '([name handler-name? doc-string? attr-map? [params*] prepost-map? body])}
  [name & fdecl]
  `(-defhandler :zou/handler :zou/handler-ns ~name ~@fdecl))

(defn invoke-with-mapper [f arg]
  (let [f (if (var? f) @f f)]
    (if-let [mapper (:zou/args-mapper (meta f))]
      (apply f (mapper arg))
      (f arg))))

;;; impls

(defmethod mapper/process-param "$req" [{:keys [sym] :as m}]
  (if (= sym '$req)
    (mapper/process-param (assoc m :sym '$request))
    (mapper/skip m)))

(defmethod mapper/process-param "$request" [{:keys [sym] :as m}]
  (if (= sym '$request)
    (assoc m :fn identity)
    (mapper/skip m)))

(defmethod mapper/process-param "$" [{:keys [sym] :as m}]
  (let [k (keyword (subs (name sym) 1))]
    (assoc m
           :fn
           #(get-in % [:zou/container k]))))

(defmethod mapper/process-param "|" [{:keys [sym] :as m}]
  (let [ks (mapv keyword (str/split (subs (name sym) 1) #"\|"))]
    (assoc m
           :alias (symbol (name (last ks)))
           :fn
           #(get-in % ks))))

(defmethod mapper/process-param nil [{:keys [sym] :as m}]
  (let [ks (map keyword (str/split (name sym) #"\|"))]
    (assoc m
           :alias (symbol (name (last ks)))
           :fn
           #(get-in % (into [:route-params] ks)
                    (get-in % (into [:params] ks))))))


;;; middleware

(defn wrap-args-mapper [handler]
  (fn [req]
    (invoke-with-mapper handler req)))

(defn args-mapper [& [conf]]
  (reify proto/RingMiddleware
    (wrap [this handler]
      (wrap-args-mapper handler))))
