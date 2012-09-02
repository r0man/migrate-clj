(ns migrate.test.sql
  (:import [java.sql SQLException])
  (:require [clj-time.core :refer [date-time]]
            [clojure.java.jdbc :as sql]
            [migrate.connection :refer [with-connection]])
  (:use clojure.test
        migrate.core
        migrate.sql
        migrate.test))

(dbtest test-create-migration-table
  (drop-migration-table)
  (is (create-migration-table)))

(dbtest test-delete-migration
  (let [migrations (find-migrations 'migrate.example)]
    (doall (map insert-migration migrations))
    (doseq [migration (reverse migrations)]
      (is (= (select-current-version) (:version migration)))
      (delete-migration migration)
      (is (not (= (select-current-version) (:version migration)))))
    (is (nil? (select-current-version)))))

(dbtest test-drop-migration-table
  (is (drop-migration-table)))

(dbtest test-insert-migration
  (doseq [migration (find-migrations 'migrate.example)]
    (insert-migration migration)
    (is (= (select-current-version) (:version migration)))))

(dbtest test-migration-table?
  (is (migration-table?))
  (drop-migration-table)
  (is (not (migration-table?))))

(dbtest test-select-current-version
  (is (nil? (select-current-version)))
  (insert-migration (nth (find-migrations 'migrate.example) 0))
  (is (= (date-time 2012 8 17 14 26) (select-current-version)))
  (insert-migration (nth (find-migrations 'migrate.example) 1))
  (is (= (date-time 2012 8 17 14 28) (select-current-version)))
  (insert-migration (nth (find-migrations 'migrate.example) 2))
  (is (= (date-time 2012 8 17 14 29) (select-current-version)))
  (drop-migration-table)
  (is (thrown? SQLException (select-current-version))))

(dbtest test-select-migrations
  (is (= [] (select-migrations)))
  (doall (map insert-migration (find-migrations 'migrate.example)))
  (is (= [(date-time 2012 8 17 14 26)
          (date-time 2012 8 17 14 28)
          (date-time 2012 8 17 14 29)]
         (map :version (select-migrations))))
  (drop-migration-table)
  (is (thrown? SQLException (select-migrations))))

(dbtest test-select-version
  (let [migration (first (find-migrations 'migrate.example))]
    (insert-migration migration)
    (let [found (select-version (:version migration))]
      (is (= (:version migration) (:version found) )))))