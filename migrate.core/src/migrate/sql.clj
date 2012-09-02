(ns migrate.sql
  (:import java.sql.SQLException)
  (:require [clojure.java.jdbc :as jdbc]
            [clj-time.core :refer [now]]
            [clj-time.coerce :refer [to-date-time to-timestamp]]))

(def ^:dynamic *migration-table* :schema-migrations)

(defn deserialize-migration
  "Deserialize the `migration` row."
  [migration]
  (-> (update-in migration [:applied-at] to-date-time)
      (update-in [:version] to-date-time)))

(defn serialize-migration
  "Serialize the `migration` row."
  [migration]
  (-> (select-keys migration [:applied-at :version])
      (update-in [:applied-at] #(to-timestamp (or %1 (now))))
      (update-in [:version] to-timestamp)))

(defn create-migration-table
  "Create the database table that holds the migration metadata."
  [table]
  (jdbc/create-table
   table
   [:version "timestamp" "not null" "unique"]
   [:applied-at "timestamp" "not null"]))

(defn drop-migration-table
  "Drop the database table that holds the migration metadata."
  [table] (jdbc/drop-table table))

(defn insert-migration
  "Insert the migration's metadata into the database."
  [table migration]
  (jdbc/insert-record table (serialize-migration migration)))

(defn delete-migration
  "Delete the migration's metadata from the database."
  [table migration]
  (jdbc/delete-rows
   (jdbc/as-identifier table)
   ["version = ?" (to-timestamp (:version migration))]))

(defn select-migration-by-version
  "Returns the current schema version, or nil if no migration has been
  run yet."
  [table version]
  (jdbc/with-query-results result-set
    [(format "SELECT * FROM %s WHERE version = ?" (jdbc/as-identifier table))
     (to-timestamp version)]
    (first (map deserialize-migration result-set))))

(defn select-current-version
  "Returns the current schema version, or nil if no migration has been
  run yet."
  [table]
  (jdbc/with-query-results result-set
    [(str "SELECT MAX(version) AS version FROM " (jdbc/as-identifier table))]
    (:version (first (map deserialize-migration result-set)))))

(defn select-migrations [table]
  (jdbc/with-query-results result-set
    [(format "SELECT * FROM %s" (jdbc/as-identifier table))]
    (doall (map deserialize-migration result-set))))

(defn table-exists? [table]
  "Returns true if the migration-table exists, otherwise false."
  (try (jdbc/with-query-results result-set
         [(format "SELECT * FROM %s LIMIT 1" (jdbc/as-identifier table))]
         (>= (count result-set) 0))
       (catch SQLException _ false)))

(defn identifier-quote-string
  "Returns the string to quote identifiers from the `connection` meta data."
  [connection] (.getIdentifierQuoteString (.getMetaData (jdbc/connection))))

(defmacro with-connection
  "Evaluates body in the context of a new connection to a database."
  [db-spec & body]
  `(jdbc/with-connection ~db-spec
     (jdbc/with-quoted-identifiers (identifier-quote-string (jdbc/connection))
       (if-not (table-exists? *migration-table*)
         (create-migration-table *migration-table*))
       ~@body)))
