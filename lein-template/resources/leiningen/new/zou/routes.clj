(ns {{namespace}}.routes)

(def main-routes ["/" {"" :main/home{{#hiccup?}}
                       ["greet-hiccup/" :people] :main/greet-hiccup{{/hiccup?}}{{#enlive?}}
                       ["greet-enlive/" :people] :main/greet-enlive{{/enlive?}}}])
