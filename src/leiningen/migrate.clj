(ns leiningen.migrate
  (:use [leiningen.help :only (help-for)]
        migrate.tasks))

(defn status
  "Show the status of the database migrations."
  [project] (print-migrations project))

(defn run
  "Run pending or a specific database migrations."
  [project & [version]] (run-migrations project version))

(defn migrate
  "Run database migrations."
  {:help-arglists '([status run])
   :subtasks [#'status #'run]}
  ([project]
     (migrate project "run"))
  ([project subtask & args]
     (cond
      (= "help" subtask) (println (help-for subtask))
      (= "run" subtask) (apply run project args)
      :else (status project))))
