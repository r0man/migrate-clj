(ns migrate.test.sql
  (:import [java.sql SQLException])
  (:require [clj-time.core :refer [date-time]]
            [clojure.java.jdbc :as jdbc])
  (:use clojure.test
        migrate.core
        migrate.sql
        migrate.test))

(deftest test-identifier-quote-string
  (with-connection test-db-spec
    (identifier-quote-string (jdbc/connection))))

(deftest test-with-connection
  (with-connection test-db-spec
    (is (jdbc/connection))
    (is (= "\"x\"" (@#'jdbc/*as-str* "x")))))

(dbtest test-create-migration-table
  (drop-migration-table *migration-table*)
  (is (create-migration-table *migration-table*)))

(dbtest test-delete-migration
  (let [migrations (find-migrations 'migrate.example)]
    (doall (map (partial insert-migration *migration-table*) migrations))
    (doseq [migration (reverse migrations)]
      (is (= (select-current-version *migration-table*) (:version migration)))
      (delete-migration *migration-table* migration)
      (is (not (= (select-current-version *migration-table*) (:version migration)))))
    (is (nil? (select-current-version *migration-table*)))))

(dbtest test-drop-migration-table
  (is (drop-migration-table *migration-table*)))

(dbtest test-insert-migration
  (doseq [migration (find-migrations 'migrate.example)]
    (insert-migration *migration-table* migration)
    (is (= (select-current-version *migration-table*) (:version migration)))))

(dbtest test-table-exists?
  (is (table-exists? *migration-table*))
  (drop-migration-table *migration-table*)
  (is (not (table-exists? *migration-table*))))

(dbtest test-select-current-version
  (is (nil? (select-current-version *migration-table*)))
  (insert-migration *migration-table* (nth (find-migrations 'migrate.example) 0))
  (is (= (date-time 2012 8 17 14 26) (select-current-version *migration-table*)))
  (insert-migration *migration-table* (nth (find-migrations 'migrate.example) 1))
  (is (= (date-time 2012 8 17 14 28) (select-current-version *migration-table*)))
  (insert-migration *migration-table* (nth (find-migrations 'migrate.example) 2))
  (is (= (date-time 2012 8 17 14 29) (select-current-version *migration-table*)))
  (drop-migration-table *migration-table*)
  (is (thrown? SQLException (select-current-version *migration-table*))))

(dbtest test-select-migrations
  (is (= [] (select-migrations *migration-table*)))
  (doall (map (partial insert-migration *migration-table*) (find-migrations 'migrate.example)))
  (is (= [(date-time 2012 8 17 14 26)
          (date-time 2012 8 17 14 28)
          (date-time 2012 8 17 14 29)]
         (map :version (select-migrations *migration-table*))))
  (drop-migration-table *migration-table*)
  (is (thrown? SQLException (select-migrations *migration-table*))))

(dbtest test-select-migration-by-version
  (let [migration (first (find-migrations 'migrate.example))]
    (insert-migration *migration-table* migration)
    (let [found (select-migration-by-version *migration-table* (:version migration))]
      (is (= (:version migration) (:version found))))))
