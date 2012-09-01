(ns leiningen.migrate
  (:require [leiningen.core.eval :refer [eval-in-project]]
            [leiningen.help :refer [help-for]]
            [leinjacker.deps :as deps]))

(defn- add-migrate-deps [project]
  (-> project
      (deps/add-if-missing '[migrate "0.1.0-SNAPSHOT"])))

(defn new
  "Create a new database migration for `db-name`."
  [project & args]
  (eval-in-project
   (add-migrate-deps project)
   `(apply migrate.task.new/new '~project '~args)
   '(require 'migrate.task.new)))

(defn status
  "Show the database migration status."
  [project]
  (prn "status")
  ;; (eval-in-project
  ;;  (extended-project project)
  ;;  `(migrate.tasks/print-migrations '~project)
  ;;  '(require 'migrate.tasks))
  )

(defn run
  "Run pending database migrations."
  [project & [version]]
  ;; (eval-in-project
  ;;  (extended-project project)
  ;;  `(migrate.tasks/run-migrations '~project '~version)
  ;;  '(require 'migrate.tasks))
  )

(defn migrate
  "Run database migrations."
  {:help-arglists '([new status run])
   :subtasks [#'new #'status #'run]}
  ([project]
     (run project nil))
  ([project subtask & args]
     (cond
      (= "new" subtask) (apply new project args)
      (= "run" subtask) (apply run project args)
      (= "status" subtask) (status project)
      :else (println (help-for subtask)))))
