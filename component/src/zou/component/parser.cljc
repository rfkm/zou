(ns zou.component.parser
  (:require [zou.component.internal.util :as cu]
            [zou.util :as u]))

(defmulti parse-component-config-entry
  (fn [component-key acc k v] k)
  :default ::default)

(defmethod parse-component-config-entry ::default [component-key acc k v]
  (assoc-in acc [:components component-key :config k] v))

(defmulti parse-system-config-entry
  (fn [acc k v] k)
  :default ::default)

(defn parse-component-config [acc k conf]
  (cond
    (map? conf)
    (reduce-kv (partial parse-component-config-entry k)
               (u/weak-assoc-in acc [:components k] {})
               conf)

    conf
    (-> acc
        (assoc-in [:components k :config] conf)
        (assoc-in [:components k :constructor] identity))

    :else
    acc))

(defmethod parse-system-config-entry ::default [acc k v]
  (parse-component-config acc k v))

(defn parse-system-config [conf]
  (reduce-kv parse-system-config-entry {} conf))

(defn component-spec [var]
  (:zou/component (meta var)))

(defn- gen-component-key [var]
  (->> (meta var)
       ((juxt :ns :name))
       (map str)
       (apply keyword)))

(defn var->spec [var]
  (let [spec (component-spec var)
        key  (keyword (:zou/name spec
                                 (gen-component-key var)))
        spec (dissoc spec :zou/name)]
    [key (u/weak-assoc spec :zou/constructor var)]))


;;;
;;; impls
;;;

(defmethod parse-system-config-entry :zou/constructor [acc _ ctor]
  (assoc-in acc [:system :constructor] ctor))

(defmethod parse-component-config-entry :zou/constructor
  [component-key acc _ v]
  (let [ctor (cu/resolve-ctor v)
        [_ spec] (if (var? ctor)
                   (var->spec ctor)
                   [nil {}])
        spec (dissoc spec :zou/constructor)]
    (-> acc
        (parse-component-config component-key spec)
        (assoc-in [:components component-key :constructor] ctor))))

(defmethod parse-component-config-entry :zou/dependencies
  [component-key acc _ deps]
  (update-in acc [:components component-key :dependencies] merge deps))

(defmethod parse-component-config-entry :zou/dependants
  [component-key acc _ inv-deps]
  (reduce-kv (fn [acc target-key alias]
               (assoc-in acc
                         [:components target-key :dependencies alias]
                         component-key))
             acc
             inv-deps))

(defmethod parse-component-config-entry :zou/optionals
  [component-key acc _ deps]
  (update-in acc [:components component-key :weak-dependencies] merge deps))

(defmethod parse-component-config-entry :zou/tags [component-key acc _ tags]
  (let [tags (map (fn [v] (if (vector? v) v [v component-key])) tags)]
    (reduce (fn [acc [tag alias]]
              (assoc-in acc
                        [:components tag :dependencies alias]
                        component-key))
            acc
            tags)))

(defmethod parse-component-config-entry :zou/disabled
  [component-key acc _ disabled?]
  (assoc-in acc [:components component-key :disabled] (some? disabled?)))
