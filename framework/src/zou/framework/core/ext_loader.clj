(ns zou.framework.core.ext-loader
  (:require [zou.component :as c]
            [zou.framework.ext :as ext]))

(defrecord ExtensionLoader []
  c/Lifecycle
  (start [this]
    (ext/initialize-extensions)
    this)
  (stop [this]
    (ext/deinitialize-extensions)
    this))
