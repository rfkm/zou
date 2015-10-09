(ns zou.web.middleware.dependency
  (:require [com.stuartsierra.dependency :as dep]
            [zou.logging :as log]
            [zou.web.middleware.proto :as proto]))

(defn dependencies [m]
  (::dependencies (meta m)))

(defn dependency-graph
  [ms]
  (reduce-kv (fn [graph key middleware]
               (reduce #(dep/depend %1 key %2)
                       graph
                       (dependencies middleware)))
             (dep/graph)
             ms))

(defn sort-middlewares [ms]
  (-> ms
      dependency-graph
      dep/topo-comparator
      (sort (keys ms))
      (->> (map #(vector % (get ms %))))))

(defn depend [ms a-id b-id]
  (if (contains? ms a-id)
    (update ms a-id vary-meta update ::dependencies conj b-id)
    ms))

(defn add [ms id middleware]
  (when (contains? ms id)
    (log/warnf "Given id '%s' already exists and an old middleware will be overwritten by the new one." id))
  (assoc ms id middleware))

(defn append [ms id m]
  (reduce (fn [ms k]
            (depend ms id k))
          (add ms id m)
          (keys ms)))

(defn prepend [ms id m]
  (reduce (fn [ms k]
            (depend ms k id))
          (add ms id m)
          (keys ms)))

(defn after [ms anchor-id id]
  (depend ms id anchor-id))

(defn before [ms anchor-id id]
  (depend ms anchor-id id))

(defn between [ms left right id]
  (-> ms
      (depend id left)
      (depend right id)))

(defn add-after [ms anchor-id id m]
  (-> ms
      (add id m)
      (after anchor-id id)))

(defn add-before [ms anchor-id id m]
  (-> ms
      (add id m)
      (before anchor-id id)))

(defn add-between [ms left right id m]
  (-> ms
      (add id m)
      (between left right id)))

(defn process-dependency-map [ms m]
  (reduce (fn [acc [type anchor id]]
            (condp = type
              :after (after acc anchor id)
              :before (before acc anchor id)
              acc))
          ms
          (for [[id v] m
                [anchor type] v]
            [type anchor id])))
