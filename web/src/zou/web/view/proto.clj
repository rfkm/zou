(ns zou.web.view.proto)

(defprotocol ViewModel
  (view-model [this]))

(extend-protocol ViewModel
  clojure.lang.APersistentMap
  (view-model [this] this))

(defprotocol Renderable
  (render [this model]))

(extend-protocol Renderable
  clojure.lang.Var
  (render [var model] (render @var model))

  clojure.lang.AFn
  (render [f model] (f model)))

(defprotocol View
  (show [this view-key model]))
