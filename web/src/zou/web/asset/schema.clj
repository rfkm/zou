(ns zou.web.asset.schema
  (:require [schema.core :as s]))

(def AssetSpec {(s/optional-key :id) s/Keyword
                :name s/Str
                :type (s/enum :javascript :stylesheet)
                :src (s/either java.net.URL java.io.File s/Str)})

(defn validate-asset-specs [assets]
  (s/validate [AssetSpec] assets))
