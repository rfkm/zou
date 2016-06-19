(ns zou.component.parser.scanner
  (:require [zou.component.parser :as p]
            [zou.util :as u]
            [zou.util.namespace :as un]
            [zou.logging :as log :include-macros true]))

#?(:clj
   (do
     (defn- find-components [prefixes]
       (un/find-tagged-vars
        :zou/component
        (->> prefixes
             (map (fn [prefix]
                    (fn [ns]
                      (.startsWith (str ns) prefix))))
             (apply some-fn))))

     (defmethod p/parse-system-config-entry
       :zou/component-scan
       [acc _ {:keys [prefixes]}]
       (->> (find-components prefixes)
            (map p/var->spec)
            (reduce (fn [acc [key spec]]
                      (p/parse-component-config acc key spec))
                    acc))))

   :cljs
   (defmethod p/parse-system-config-entry
     :zou/component-scan
     [acc _ {:keys [prefixes]}]
     (log/warn ":zou/component-scan key will be ignored since the component scanner does not support CLJS.")
     acc))
