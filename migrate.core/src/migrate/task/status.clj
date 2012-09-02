(ns migrate.task.status
  (:require [migrate.core :refer [find-migrations]]
            [migrate.sql :refer [select-migration-by-version *migration-table*]]
            [migrate.util :refer [format-human-time]]))

(defn status
  "Print the status of all migrations in `base-ns`."
  [base-ns]
  (let [pattern "%-32s %-8s %-32s %s"]
    (println (format pattern "VERSION" "STATUS" "WHEN" "DESCRIPTION"))
    (println (apply str (repeat 120 "-")))
    (doseq [migration (find-migrations base-ns)
            :let [found (select-migration-by-version *migration-table* (:version migration))]]
      (-> (format
           pattern
           (format-human-time (:version migration))
           (if found "DONE" "PENDING")
           (or (format-human-time (:applied-at found)) "-")
           (:doc (meta (:up migration))))
          (println)))))