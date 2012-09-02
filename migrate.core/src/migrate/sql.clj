(ns migrate.sql
  (:import java.sql.SQLException)
  (:require [clojure.java.jdbc :as jdbc]
            [clj-time.core :refer [now]]
            [clj-time.coerce :refer [to-date-time to-timestamp]]))

(def ^:dynamic *migration-table* :schema-migrations)

(defn create-migration-table
  "Create the database table that holds the migration metadata."
  []
  (jdbc/create-table
   *migration-table*
   [:version "timestamp" "not null" "unique"]
   [:description "text"]
   [:created-at "timestamp" "not null"]))

(defn drop-migration-table
  "Drop the database table that holds the migration metadata."
  [] (jdbc/drop-table *migration-table*))

(defn insert-migration
  "Insert the migration's metadata into the database."
  [migration]
  (jdbc/insert-record
   *migration-table*
   {:created-at (to-timestamp (now))
    :version (to-timestamp (:version migration))}))

(defn delete-migration
  "Delete the migration's metadata from the database."
  [migration]
  (jdbc/delete-rows
   (jdbc/as-identifier *migration-table*)
   ["version=?" (to-timestamp (:version migration))]))

(defn select-version
  "Returns the current schema version, or nil if no migration has been
  run yet."
  [version]
  (jdbc/with-query-results result-set
    [(format "SELECT * FROM %s WHERE version = ?" (jdbc/as-identifier *migration-table*))
     (to-timestamp version)]
    (if-let [migration (first result-set)]
      (-> (update-in migration [:created-at] to-date-time)
          (update-in [:version] to-date-time)))))

(defn select-current-version
  "Returns the current schema version, or nil if no migration has been
  run yet."
  []
  (jdbc/with-query-results result-set
    [(str "SELECT MAX(version) AS version FROM " (jdbc/as-identifier *migration-table*))]
    (to-date-time (:version (first result-set)))))

(defn select-migrations []
  (jdbc/with-query-results result-set
    [(format "SELECT * FROM %s" (jdbc/as-identifier *migration-table*))]
    (into [] (map #(assoc %1 :version (to-date-time (:version %1))) result-set))))

(defn migration-table? []
  "Returns true if the migration-table exists, otherwise false."
  (try (do (select-current-version) true)
       (catch SQLException _ false)))