(ns {{namespace}}.view.main
  (:require [zou.util :as u]
            [clojure.string :as str]{{#enlive?}}
            [clojure.java.io :as io]
            [net.cgrand.enlive-html :as html]{{/enlive?}}{{#hiccup?}}
            [hiccup.core :as h]
            [hiccup.page :as hp]{{/hiccup?}}))

(u/defnk home [app-name href {javascripts []} {stylesheets []}]
  (str "<!DOCTYPE html><html><head>"
       "<title>" app-name "</title>"
       (str/join (for [s stylesheets]
                   (str "<link rel=\"stylesheet\" href=\"" (href s) "\">")))
       "</head><body>"
       "<h1>" app-name "</h1>"
       "<p>Welcome to Zou!</p>"
       (str/join (for [j javascripts]
                   (str "<script src=\"" (href j) "\"></script>")))
       "</body></html>")){{#hiccup?}}

(u/defnk hiccup [app-name header message href {javascripts []} {stylesheets []}]
  (hp/html5
   [:head
    [:title app-name]
    (apply hp/include-css (map href stylesheets))]
   [:body
    [:h1 header]
    [:p (h/h message)]
    (apply hp/include-js (map href javascripts))])){{/hiccup?}}{{#enlive?}}

(html/deftemplate enlive (io/resource "templates/layout.html")
  [{:keys [app-name header message javascripts stylesheets href]}]
  [:title] (html/content app-name)
  [:head] (html/append (html/html (for [s stylesheets]
                                    [:link {:rel "stylesheet" :href (href s)}])))
  [:body] (html/append (html/html (for [j javascripts]
                                    [:script {:src (href j)}])))
  [:h1] (html/content header)
  [:#content] (html/content message)){{/enlive?}}
