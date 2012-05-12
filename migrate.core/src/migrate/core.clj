(ns migrate.core
  (:import java.sql.SQLException)
  (:require [clojure.java.jdbc :as jdbc])
  (:use [clj-time.core :only (now)]
        [clj-time.coerce :only (to-date-time to-timestamp to-long)]
        [clj-time.format :only (formatters unparse)]
        [clojure.tools.logging :only (info)]
        [clojure.string :only (blank?)]
        [environ.core :only (env)]))

(def ^:dynamic *migrations* (atom {}))
(def migration-table "schema_migrations")

(defn str< [s1 s2]
  (< (.compareTo s1 s2) 0))

(defn str<= [s1 s2]
  (<= (.compareTo s1 s2) 0))

(defn format-time [date]
  (if date (unparse (formatters :date-time-no-ms) (to-date-time date))))

(defn create-migration-table
  "Create the database table that holds the migration metadata."
  [] (jdbc/create-table
      migration-table
      [:version :timestamp "PRIMARY KEY NOT NULL"]
      [:description :text]
      [:created_at :timestamp "NOT NULL"]))

(defn drop-migration-table
  "Drop the database table that holds the migration metadata."
  [] (jdbc/drop-table migration-table))

(defn insert-migration
  "Insert the migration's metadata into the database."
  [migration]
  (jdbc/insert-records
   migration-table
   (-> (assoc migration
         :created_at (to-timestamp (now))
         :version (to-timestamp (:version migration)))
       (dissoc :up :down))))

(defn delete-migration
  "Delete the migration's metadata from the database."
  [migration] (jdbc/delete-rows migration-table ["version=?" (to-timestamp (:version migration))]))

(defn latest-migration
  "Returns the latest (the most recent) migration."
  [] (last (sort-by :version (vals @*migrations*))))

(defn latest-version
  "Returns the version of the latest (the most recent) migration."
  [] (:version (latest-migration)))

(defn select-current-version
  "Returns the current schema version, or nil if no migration has been
  run yet."
  [] (jdbc/with-query-results result-set
       [(str "SELECT MAX(version) AS version FROM " migration-table)]
       (to-date-time (:version (first result-set)))))

(defn migration-table? []
  "Returns true if the migration-table exists, otherwise false."
  (try (do (select-current-version) true)
       (catch SQLException _ false)))

(defn select-migrations []
  (if (migration-table?)
    (jdbc/with-query-results result-set
      [(format "SELECT * FROM %s" migration-table)]
      (into [] (map #(assoc %1 :version (to-date-time (:version %1))) result-set)))))

(defmacro defmigration [version description up-form down-form]
  `(if-let [version# (to-date-time ~version)]
     (let [migration# {:version version# :description ~description :up #(~@up-form) :down #(~@down-form)}]
       (swap! *migrations* assoc (:version migration#) migration#))
     (throw (Exception. (str "Invalid migration version. Must be a timestamp: " ~version)))))

(defn find-applicable-migrations
  "Returns all migrations that have to be run to migrate from
  from-version to to-version."
  [migrations from-version to-version]
  (let [from-version (to-long from-version)
        to-version (to-long to-version)]
    (if (or (nil? from-version)
            (and to-version (<= from-version to-version)))
      (filter #(and (or (nil? from-version)
                        (< from-version (to-long (:version %))))
                    (or (nil? to-version)
                        (<= (to-long (:version %)) to-version)))
              (sort-by :version migrations))
      (reverse (find-applicable-migrations migrations to-version from-version)))))

(defn find-migration-by-version
  "Returns the migration with the given version."
  [version]
  (if-let [version (to-date-time version)]
    (first (filter #(= (:version %) version) (vals @*migrations*)))
    (throw (Exception. (str "Invalid migration version. Must be a timestamp: " version)))))

(defn- run-up
  "Run the migration by invoking the fn stored under the :up key and
  insert the metadata into the migration table."
  [migration]
  (info (str "Migrating up: " (:version migration)))
  ((:up migration))
  (insert-migration migration))

(defn- run-down
  "Run the migration by invoking the fn stored under the :down key and
  delete the metadata into the migration table."
  [migration]
  (info (str "Migrating down: " (:version migration)))
  ((:down migration))
  (delete-migration migration))

(defn- direction [current-version target-version]
  (if (or (nil? current-version)
          (<= (to-long current-version)
              (to-long target-version)))
    :up :down))

(defn run
  "Run all database migrations to get from the current to the target
  version. Providing 0 as the target version runs all migrations down
  starting with the current."
  [& [target-version]]
  (if-not (migration-table?)
    (create-migration-table))
  (let [current-version (select-current-version)
        target-version (or (and (= target-version 0) 0) target-version (latest-version))]
    (jdbc/transaction
     (doseq [migration (find-applicable-migrations (vals @*migrations*) current-version target-version)]
       (if (= (direction current-version target-version) :up)
         (run-up migration)
         (run-down migration))
       (info (str "   Description: " (:description migration)))))))

(defmacro with-connection
  "Eval `body` within the context of the current environment's
  database connection."
  [& body]
  `(let [connection# (env :database-url)]
     (if (blank? connection#)
       (throw (Exception. "Please set the DATABASE_URL environment variable.")))
     (jdbc/with-connection connection#
       ~@body)))
