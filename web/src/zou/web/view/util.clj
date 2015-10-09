(ns zou.web.view.util
  (:require [zou.web.view.proto :as proto]))

(defn merge-models [& models]
  (->> models
       (filter #(satisfies? proto/ViewModel %))
       (map proto/view-model)
       (apply merge)))
