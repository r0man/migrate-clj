(ns leiningen.migrate
  (:use [leiningen.core.eval :only (eval-in-project)]
        [leiningen.help :only (help help-for)]
        migrate.tasks))

(defn- extended-project [project]
  (update-in project [:dependencies] conj ['migrate "0.0.9-SNAPSHOT"]))

(defn status
  "Show the database migration status."
  [project]
  (eval-in-project
   (extended-project project)
   `(migrate.tasks/print-migrations '~project)
   '(require 'migrate.tasks)))

(defn run
  "Run pending database migrations."
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
     (run project nil))
  ([project subtask & args]
     (cond
      (= "run" subtask) (apply run project args)
      (= "status" subtask) (status project)
      :else (println (help-for subtask)))))
