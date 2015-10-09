(ns zou-todo.view-helper
  (:require [zou.util :as u]
            [zou.web.routing :as r]
            [zou.web.view.proto :as vproto]))

(u/defnk view-model [router]
  {:href
   (fn [p & [params]]
     (r/href router p params))})

(defrecord ViewHelper [router]
  vproto/ViewModel
  (view-model [this]
    (#'view-model this)))
