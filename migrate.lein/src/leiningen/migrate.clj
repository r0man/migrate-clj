(ns leiningen.migrate
  (:use [leiningen.core.eval :only (eval-in-project)]
        [leiningen.help :only (help-for)]
        migrate.tasks))

(defn- extended-project [project]
  (update-in project [:dependencies] conj ['migrate "0.0.9-SNAPSHOT"]))

(defn status
  "Show the status of the database migrations."
  [project]
  (eval-in-project
   (extended-project project)
   `(migrate.tasks/print-migrations '~project)
   '(require 'migrate.tasks)))

(defn run
  "Run pending or a specific database migrations."
  [project & [version]]
  (eval-in-project
   (extended-project project)
   `(migrate.tasks/run-migrations '~project '~version)
   '(require 'migrate.tasks)))

(defn migrate
  "Run database migrations."
  {:help-arglists '([status run])
   :subtasks [#'status #'run]}
  ([project]
     (migrate project "status"))
  ([project subtask & args]
     (cond
      (= "help" subtask) (println (help-for subtask))
      (= "run" subtask) (apply run project args)
      :else (status project))))
