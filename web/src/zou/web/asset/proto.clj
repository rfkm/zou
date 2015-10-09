(ns zou.web.asset.proto)

(defprotocol AssetsProvider
  (assets [this]
    "Returns spec of asset.
     E.g. [{:name \"/js/a.js\" :type :javascript :src \"resources/public/js/a.js/\"}
           {:name \"/css/b.css\" :type :stylesheet :src (io/resource \"public/css/b.css\")}]"))
