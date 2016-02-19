(ns zou.util.platform.cljc
  (:require
   #?@(:clj [[zou.util.platform :as up]]
       :cljs [[zou.util.platform.cljs :include-macros true]
              [zou.util.platform :as up :include-macros true]])))

#?(:clj (up/if-cljs
         (require 'zou.util.platform.cljs)
         (require 'zou.util.platform.clj)))

#?(:clj
   (up/if-cljs
    (def my-var :clj-cljs)
    (def my-var :clj-clj))

   :cljs
   (up/if-cljs
    (def my-var :cljs-cljs)
    (def my-var :cljs-clj)))

(defmacro m []
  `(up/if-cljs
    (zou.util.platform.cljs/m)
    (zou.util.platform.clj/m)))
