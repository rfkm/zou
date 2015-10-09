(ns zou-todo.view.todo
  (:require [hiccup.core :as h]
            [hiccup.element :as he]
            [hiccup.form :as hf]
            [hiccup.page :as hp]
            [ring.util.anti-forgery :as af]
            [zou.util :as u]))

(u/defnk head [href stylesheets]
  [:head
   (apply hp/include-css (map href stylesheets))])

(u/defnk footer [href javascripts]
  (apply hp/include-js (map href javascripts)))

(defn page [m & contents]
  (hp/html5
   (head m)
   [:body
    contents
    (footer m)]))

(u/defnk index [href javascripts stylesheets tasks :as m]
  (page m
        [:h1 "Tasks"]
        (if (seq tasks)
          (he/ordered-list
           (for [t tasks
                 :let [{:keys [done? title id]} t]]
             (list
              (->> [:span (he/link-to (href :todo/edit {:task-id id}) (h/h title))]
                   (u/?>> done? (vector :s)))

              [:ul
               [:li (if done?
                      (he/link-to (href :todo/undone {:task-id id}) "Undone")
                      (he/link-to (href :todo/done {:task-id id}) "Done"))]

               [:li (he/link-to (href :todo/delete {:task-id id}) "Del")]]))))
        (hf/form-to [:post (href :todo/create)]
                    (hf/text-field :title)
                    (af/anti-forgery-field)
                    (hf/submit-button "Create new task"))))

(u/defnk edit [href javascripts stylesheets task :as m]
  (page m
        [:h1 "Task"]
        (hf/form-to [:put (href :todo/update {:task-id (:id task)})]
                    (hf/text-field :title (:title task))
                    (af/anti-forgery-field)
                    (hf/submit-button "Update task"))))
