(ns leiningen.migrate
  (:refer-clojure :exclude [replace])
  (:require [clojure.string :refer [replace]]
            [leiningen.core.eval :refer [eval-in-project]]
            [leiningen.help :refer [help-for]]
            [leinjacker.deps :as deps]
            [migrate.util :refer [with-base-ns]]
            leiningen.run))

(defn- add-migrate-deps [project]
  (-> project
      (deps/add-if-missing '[migrate/migrate.core "0.1.0-SNAPSHOT"])))

(defn run-migrate [project command & args]
  (apply leiningen.run/run (add-migrate-deps project) "-m" "migrate.tool" (name command) (map str args)))

(defn new
  "Create a new database migration."
  [project db-name & args]
  (with-base-ns [project db-name base-ns]
    (apply run-migrate project :new
           "--directory" (first (:source-paths project))
           "--namespace" base-ns
           args)))

(defn status
  "Show the database migration status."
  [project db-name]
  (with-base-ns [project db-name base-ns]
    (run-migrate project :status
                 "--database" db-name
                 "--namespace" base-ns)))

(defn run
  "Run the database migrations."
  [project & args]
  (let [[db-name & [version]] args]
    (with-base-ns [project db-name base-ns]
      (apply run-migrate project :run
             "--database" db-name
             "--namespace" base-ns
             (if version ["--version" version])))))

(defn migrate
  "Run database migrations."
  {:help-arglists '([new status run])
   :subtasks [#'new #'status #'run]}
  [project & [subtask & args]]
  (condp = subtask
    "new" (apply new project args)
    "status" (apply status project args)
    "run" (apply run project args)))
