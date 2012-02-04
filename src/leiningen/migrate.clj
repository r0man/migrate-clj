(ns leiningen.migrate
  (:use [leiningen.help :only (help-for)]
        migrate.tasks))

(defn status
  "Show the status of the database migrations."
  [project & [env]] (print-migrations project env))

(defn run
  "Apply the necessary database migrations."
  [project & [version]] (run-migrations project version))

(defn migrate
  "Leiningen database migrations."
  {:help-arglists '([status run])
   :subtasks [#'status #'run]}
  ([project]
     (migrate project "run"))
  ([project subtask & args]
     (cond
      (= "help" subtask) (println (help-for "env"))
      (= "status" subtask) (apply status project args)
      :else (apply run project args))))
