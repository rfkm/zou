(ns {{namespace}}.view-helper
    (:require [zou.web.routing :as r]
              [zou.web.view.proto :as vproto]))

(defrecord ViewHelper [router]
  vproto/ViewModel
  (view-model [this]
    {:href (partial r/href router)}))
