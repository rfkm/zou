(ns zou.web.handler.args-mapper)

(declare process-param)

(defn- dispatch-param-processor [{:keys [sym] :as m}]
  (some #(and (.startsWith (name sym) (name %)) %)
        (sort #(compare
                (count (name %2))
                (count (name %1)))
              (->> process-param
                   methods
                   keys
                   (remove nil?)
                   (remove (::skip (meta m) #{}))))))

(defmulti process-param #'dispatch-param-processor)

(defmulti process-infix :infix)

(defmethod process-infix :default [m]
  (throw (java.lang.IllegalArgumentException.
          (str "Unknown infix operator: " (:infix m)))))

(defmulti coercer identity)

(defmethod coercer :default [type]
  (throw (java.lang.IllegalArgumentException.
          (str "Unknown coercer: " type))))

(defn skip [m]
  (let [prefix (dispatch-param-processor m)]
    (process-param (vary-meta m update-in [::skip] (fnil conj #{}) prefix))))

(defn- group-params [params]
  (loop [[x & xs] params acc []]
    (if x
      (cond
        (symbol? x)
        (recur xs (conj acc x))

        (and (keyword? x) (seq acc) (first xs))
        (recur (next xs)
               (conj (vec (butlast acc))
                     [(peek acc) x (first xs)]))

        :else
        (throw (ex-info "Invalid format of parameters" {:params params})))
      acc)))

(defn- merge-spec-map [m m']
  (assoc (merge m m')
         :fn (comp (:fn m' identity) (:fn m identity))))

(defn- process-infixes [m]
  (loop [{:keys [sym] :as m} m]
    (if (or (vector? sym) (list? sym))
      (recur (assoc (merge-spec-map m (process-infix m))
                    :sym (first sym)
                    :infix (second sym)
                    :infix-operand (last sym)))
      (dissoc (merge-spec-map m (process-infix m))
              :infix
              :infix-operand))))

(defn- gen-sym-fn [[sym infix infix-operand :as p]]
  (let [m (if (= (count p) 1)           ; no infix
            {:sym sym}
            (process-infixes {:infix-operand infix-operand
                              :sym sym
                              :infix infix}))]
    (merge-spec-map (process-param m) m)))

(defn gen-destructuring-spec [params]
  (let [ms (map (fn [p]
                  (gen-sym-fn
                   (if (vector? p)
                     p
                     [p])))
                (group-params params))]
    {:params (mapv #(:alias % (:sym %)) ms)
     :fn (if (seq ms) (apply juxt (map :fn ms)) (constantly []))}))


;; impls

(defmethod process-infix :as [m]
  (assert (symbol? (:infix-operand m)) "An alias must be a symbol")
  (assoc m
         :alias
         (:infix-operand m)))

(defmethod process-infix :<< [m]
  (assoc m
         :fn (coercer (:infix-operand m))))

(defmethod coercer 'as-long [_]
  (fn [v]
    (when v
      (Long/parseLong v))))
