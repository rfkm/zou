(ns zou.fixtures.enlive-ns
  (:require [clojure.java.io :as io]
            [net.cgrand.enlive-html :as html]))

(def tmpl "target/enlive_test.html")

(when-not (.exists (io/file tmpl))
  (spit tmpl "<div></div>"))

(html/deftemplate render (io/file tmpl)
  [])
