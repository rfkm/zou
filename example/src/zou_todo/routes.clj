(ns zou-todo.routes)

(def main-routes ["/" {"" :todo/index
                       "create"  {:post :todo/create}
                       [[long :task-id]]  {"/edit" {:get :todo/edit
                                                    :put :todo/update}
                                           "/undone" :todo/undone
                                           "/done" :todo/done
                                           "/del" :todo/delete}
                       }])
