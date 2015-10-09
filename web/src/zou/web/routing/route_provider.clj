(ns zou.web.routing.route-provider
  (:require [bidi.bidi :as bidi]
            [zou.util.namespace :as un]))

(defrecord SimpleRouteProvider [spec]
  bidi/RouteProvider
  (routes [this]
    (cond
      (var? spec) (deref spec)
      (symbol? spec) (deref (un/resolve-var spec))
      (fn? spec) (spec)
      :else spec)))
