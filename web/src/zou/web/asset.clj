(ns zou.web.asset
  (:require [bidi.bidi :as bidi]
            [clojure.java.io :as io]
            [zou.util :as u]
            [zou.web.asset.proto :as aproto]
            [zou.web.asset.schema :as s]
            [zou.web.response :as res]
            [zou.web.view.proto :as vproto]))

(defn route-id [{:keys [id name]}]
  (keyword (or id name)))

;;; TODO: cleanup
(defrecord AssetManager [asset-providers]
  bidi/RouteProvider
  (routes [this]
    (let [specs (->> asset-providers
                     vals
                     (mapcat aproto/assets)
                     s/validate-asset-specs
                     (mapv (fn [{:keys [id name type src] :as spec}]
                             [name (-> (cond
                                         (instance? java.io.File src) (fn [req] {:body src})
                                         (string? src) (fn [req] (let [f (io/file src)]
                                                                   (when-not (.exists f)
                                                                     (res/not-found!))
                                                                   {:body f}))
                                         (instance? java.net.URL src) (fn [req] (res/url-response src))
                                         :else (fn [req] (res/not-found!)))
                                       (bidi/tag (route-id spec)))])))]
      ["" specs]))

  vproto/ViewModel
  (view-model [this]
    (let [specs (->> asset-providers
                     vals
                     (mapcat aproto/assets)
                     s/validate-asset-specs
                     (group-by :type)
                     (u/map-vals (partial map route-id)))]
      {:javascripts (:javascript specs [])
       :stylesheets (:stylesheet specs [])})))

(defrecord StaticAssetProvider [specs]
  aproto/AssetsProvider
  (assets [this]
    specs))

(defrecord CljsbuildAssetProvider [builds]
  aproto/AssetsProvider
  (assets [this]
    (let [builds (if (map? builds)
                   (map (fn [[k v]] (assoc v :id k)) builds)
                   builds)]
      (map (fn [v]
             {:name (:serve-path v)
              :type :javascript
              :src (or (get-in v [:build-options :output-to])
                       (get-in v [:compiler :output-to]))})
           builds))))
