(ns ^{:author "Roman Scherer"
      :doc "" }
  migrate.core 
  (:import java.sql.SQLException)
  (:require [clojure.contrib.sql :as sql])
  (:use [clojure.contrib.def :only (defvar)]
        [clojure.contrib.logging :only (info)]))

(defvar *migrations* (atom {})
  "All migrations by name.")

(defvar migration-table "schema_migrations"
  "The table name for the migration metadata.")

(defn str= [s1 s2] (= (.compareTo s1 s2) 0))
(defn str< [s1 s2] (< (.compareTo s1 s2) 0))
(defn str> [s1 s2] (> (.compareTo s1 s2) 0))
(defn str<= [s1 s2] (<= (.compareTo s1 s2) 0))
(defn str>= [s1 s2] (>= (.compareTo s1 s2) 0))

(defn create-migration-table
  "Create the database table that holds the migration metadata."
  [] (sql/create-table
      migration-table
      [:version :string "PRIMARY KEY NOT NULL"]
      [:description :string]
      [:created_at :time]))

(defn drop-migration-table
  "Drop the database table that holds the migration metadata."
  [] (sql/drop-table migration-table))

(defn insert-migration
  "Insert the migration's metadata into the database."
  [migration]  
  (sql/insert-records
   migration-table
   (-> migration
       (assoc :created_at (java.util.Date.))
       (dissoc :up :down))))

(defn delete-migration
  "Delete the migration's metadata from the database."
  [migration] (sql/delete-rows migration-table ["version=?" (:version migration)]))

(defn latest-migration
  "Returns the latest (the most recent) migration."
  [] (last (sort-by :version (vals @*migrations*))))

(defn latest-version
  "Returns the version of the latest (the most recent) migration."
  [] (:version (latest-migration)))

(defmacro defmigration [name description up-form down-form]
  `(let [migration# {:version ~name :description ~description :up #(~@up-form) :down #(~@down-form)}]
     (swap! *migrations* assoc (:version migration#) migration#)))

(defn find-applicable-migrations
  "Returns all migrations that have to be run to migrate from
  from-version to to-version."
  [from-version to-version]
  (let [from-version (or from-version "")
        to-version (:version (latest-migration))]
    (if (str<= from-version to-version)
      (filter #(and (str< from-version (:version %)) (str<= (:version %) to-version))
              (sort-by :version (vals @*migrations*)))
      (reverse (find-applicable-migrations to-version from-version)))))

(defn find-migration-by-version
  "Returns the migration with the given version."
  [version] (first (filter #(= (:version %) version) (vals @*migrations*))))

(defn select-current-version
  "Returns the current schema version, or nil if no migration has been
  run yet."
  [] (sql/with-query-results result-set
       [(str "SELECT MAX(version) AS version FROM " migration-table)]
       (:version (first result-set))))

(defn migration-table? []
  "Returns true if the migration-table exists, otherwise false."
  (try (do (select-current-version) true)
       (catch SQLException _ false)))

(defn- run-up
  "Run the migration by invoking the fn stored under the :up key and
  insert the metadata into the migration table."
  [migration]
  ((:up migration))
  (insert-migration migration))

(defn- run-down 
  "Run the migration by invoking the fn stored under the :down key and
  delete the metadata into the migration table."
  [migration]
  ((:down migration))
  (delete-migration migration))

(defn run
  "Run the migrations from the current to the target version. If
  target version is nil the migartions run up to the latest version."
  [& [target-version]]
  (if-not (migration-table?)
    (create-migration-table))
  (let [current-version (or (select-current-version) "")
        target-version (or target-version (latest-version))]
    (sql/transaction
     (doseq [migration (find-applicable-migrations current-version target-version)]
       (info (str "Running migration: " (:version migration)))
       (info (str "      Description: " (:description migration)))
       (if (str<= current-version target-version )
         (run-up migration)
         (run-down migration))))))
