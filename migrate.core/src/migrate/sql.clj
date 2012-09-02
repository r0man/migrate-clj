(ns migrate.sql
  (:import java.sql.SQLException)
  (:require [clojure.java.jdbc :as jdbc]
            [clj-time.core :refer [now]]
            [clj-time.coerce :refer [to-date-time to-timestamp]]))

(defn create-migration-table
  "Create the database table that holds the migration metadata."
  [table]
  (jdbc/create-table
   table
   [:version "timestamp" "not null" "unique"]
   [:description "text"]
   [:created-at "timestamp" "not null"]))

(defn drop-migration-table
  "Drop the database table that holds the migration metadata."
  [table] (jdbc/drop-table table))

(defn insert-migration
  "Insert the migration's metadata into the database."
  [table migration]
  (jdbc/insert-record
   table
   {:created-at (to-timestamp (now))
    :version (to-timestamp (:version migration))}))

(defn delete-migration
  "Delete the migration's metadata from the database."
  [table migration]
  (jdbc/delete-rows
   (jdbc/as-identifier table)
   ["version=?" (to-timestamp (:version migration))]))

(defn select-migration-by-version
  "Returns the current schema version, or nil if no migration has been
  run yet."
  [table version]
  (jdbc/with-query-results result-set
    [(format "SELECT * FROM %s WHERE version = ?" (jdbc/as-identifier table))
     (to-timestamp version)]
    (if-let [migration (first result-set)]
      (-> (update-in migration [:created-at] to-date-time)
          (update-in [:version] to-date-time)))))

(defn select-current-version
  "Returns the current schema version, or nil if no migration has been
  run yet."
  [table]
  (jdbc/with-query-results result-set
    [(str "SELECT MAX(version) AS version FROM " (jdbc/as-identifier table))]
    (to-date-time (:version (first result-set)))))

(defn select-migrations [table]
  (jdbc/with-query-results result-set
    [(format "SELECT * FROM %s" (jdbc/as-identifier table))]
    (doall (map #(assoc %1 :version (to-date-time (:version %1))) result-set))))

(defn table-exists? [table]
  "Returns true if the migration-table exists, otherwise false."
  (try (jdbc/with-query-results result-set
         [(format "SELECT * FROM %s LIMIT 1" (jdbc/as-identifier table))]
         (>= (count result-set) 0))
       (catch SQLException _ false)))