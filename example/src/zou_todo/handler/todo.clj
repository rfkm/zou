(ns zou-todo.handler.todo
  (:refer-clojure :exclude [update])
  (:require [zou.web.handler :as h]
            [zou.web.response :as res]
            [zou.web.routing :as r]))

(defonce db (atom [{:id 1 :title "あれする" :done? true}
                   {:id 2 :title "あれする2" :done? false}
                   {:id 3 :title "あれ<b>する</b>3" :done? false}]))

(h/defhandler index [$view]
  (-> ($view :todo/index {:tasks (sort-by :id @db)})
      res/ok
      res/html))

(h/defhandler create [$router title]
  (swap! db (fn [old] (conj old
                            {:id (inc (reduce max 0 (map :id old)))
                             :title title})))
  (res/see-other (r/href $router :todo/index)))

(h/defhandler edit [$view task-id]
  (-> ($view :todo/edit {:task (first (filter #(= (:id %) task-id) @db))})
      res/ok
      res/html))

(defn- update-db-by-task-id! [task-id f]
  (swap! db (fn [old]
              (filter identity
                      (reduce (fn [acc m]
                                (if (= (:id m) task-id)
                                  (f m)
                                  m))
                              []
                              old)))))

(h/defhandler update [$router task-id title]
  (update-db-by-task-id! task-id #(assoc % :title title))
  (res/see-other (r/href $router :todo/index)))

(h/defhandler done [$router task-id]
  (update-db-by-task-id! task-id #(assoc % :done? true))
  (res/see-other (r/href $router :todo/index)))

(h/defhandler undone [$router task-id]
  (update-db-by-task-id! task-id #(assoc % :done? false))
  (res/see-other (r/href $router :todo/index)))

(h/defhandler delete [$router task-id]
  (update-db-by-task-id! task-id (constantly nil))
  (res/see-other (r/href $router :todo/index)))
