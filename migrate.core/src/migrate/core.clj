(ns migrate.core
  (:require [clj-time.core :refer [now]]
            [clj-time.coerce :refer [to-date-time to-timestamp to-long]]
            [clojure.java.classpath :refer [classpath]]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :refer [blank?]]
            [clojure.tools.logging :refer [info]]
            [commandline.core :refer [print-help with-commandline]]
            [migrate.sql :refer :all]
            [migrate.util :refer [format-time parse-time re-ns-matches]]))

(defrecord Migration [ns version up down])

(defn direction
  "Returns the direction keyword to get from `current-version` to
  `target-version`."
  [current-version target-version]
  (if (or (nil? current-version)
          (<= (to-long current-version)
              (to-long target-version)))
    :up :down))

(defn require-migration
  "Make a new migration from ns."
  [ns]
  (require ns)
  (map->Migration
   {:ns ns
    :version (parse-time ns)
    :up (ns-resolve ns 'up)
    :down (ns-resolve ns 'down)}))

(defn find-migrations
  "Find all migrations under namespace `ns`."
  [ns] (->> (re-ns-matches (re-pattern (str ns "\\..*")))
            (map require-migration)
            (sort-by :version)))

(defn latest-migration
  "Returns the latest (the most recent) migration."
  [ns] (last (sort-by :version (find-migrations ns))))

(defn latest-version
  "Returns the version of the latest (the most recent) migration."
  [ns] (:version (latest-migration ns)))

(defn target-version
  "Returns the target version of the migrations in `ns`."
  [ns version]
  (cond
   (or (= 0 version)
       (= "0" version))
   0
   (to-date-time version)
   (to-date-time version)
   :else (latest-version ns)))

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
  [ns version]
  (if-let [version (to-date-time version)]
    (first (filter #(= (:version %) version) (find-migrations ns)))))

(defn print-migrations [ns]
  (let [pattern "%-32s %-8s %-32s %s"]
    (println (format pattern "VERSION" "STATUS" "WHEN" "DESCRIPTION"))
    (println (apply str (repeat 120 "-")))
    (doseq [migration (find-migrations ns)
            :let [found (select-migration-by-version *migration-table* (:version migration))]]
      (-> (format
           pattern
           (format-time (:version migration))
           (if found "DONE" "PENDING")
           (or (format-time (:applied-at found)) "-")
           (:doc (meta (:up migration))))
          (println)))))

(defn run-up
  "Run the migration by invoking the fn stored under the :up key and
  insert the metadata into the migration table."
  [migration]
  (info (str "+ " (format-time (:version migration)) " " (:doc (meta (:up migration)))))
  ((:up migration))
  (insert-migration *migration-table* migration))

(defn run-down
  "Run the migration by invoking the fn stored under the :down key and
  delete the metadata into the migration table."
  [migration]
  (info (str "- " (format-time (:version migration)) " " (:doc (meta (:down migration)))))
  ((:down migration))
  (delete-migration *migration-table* migration))

(defn run
  "Run all database migrations to get from the current to the target
  version. Providing 0 as the target version runs all migrations down
  starting with the current."
  [ns & [version]]
  (if-not (table-exists? *migration-table*)
    (create-migration-table *migration-table*))
  (let [current-version (select-current-version *migration-table*)
        target-version (target-version ns version)]
    (jdbc/transaction
     (info (format "Running migrations in %s from %s to %s." ns current-version target-version))
     (doseq [migration (find-applicable-migrations (find-migrations ns) current-version target-version)]
       (if (= (direction current-version target-version) :up)
         (run-up migration)
         (run-down migration))))))
