(ns {{namespace}}.handler.main
  (:require [zou.web.handler :as h]
            [zou.web.response :as res]))

(h/defhandler home [$view]
  (-> ($view :main/home)
      res/ok
      res/html)){{#hiccup?}}

(h/defhandler greet-hiccup [$view people]
  (-> ($view :main/hiccup
             {:header "Hiccup test"
              :message (str "Hello, " people)})
      res/ok
      res/html)){{/hiccup?}}{{#enlive?}}

(h/defhandler greet-enlive [$view people]
  (-> ($view :main/enlive
             {:header "Enlive test"
              :message (str "Hello, " people)})
      res/ok
      res/html)){{/enlive?}}
