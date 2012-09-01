(ns leiningen.migrate
  (:require [leiningen.core.eval :refer [eval-in-project]]
            [leiningen.help :refer [help-for]]))

(defn- extended-project [project]
  (update-in project [:dependencies] conj ['migrate "0.1.0-SNAPSHOT"]))

(defn create
  "Create a new database migration."
  [project]
  (prn "create")
  ;; (eval-in-project
  ;;  (extended-project project)
  ;;  `(migrate.tasks/print-migrations '~project)
  ;;  '(require 'migrate.tasks))
  )

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
  {:help-arglists '([create status run])
   :subtasks [#'create #'status #'run]}
  ([project]
     (run project nil))
  ([project subtask & args]
     (cond
      (= "run" subtask) (apply run project args)
      (= "status" subtask) (status project)
      :else (println (help-for subtask)))))
