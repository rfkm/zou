(ns zou.web.response
  (:require [zou.util.namespace :as un]))

(un/import-ns ring.util.http-response)

(defn html [resp]
  (content-type resp "text/html"))

(defn json [resp]
  (content-type resp "application/json"))
