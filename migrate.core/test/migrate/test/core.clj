(ns migrate.test.core
  (:import [java.sql DriverManager SQLException])
  (:require [clojure.java.jdbc :as sql]
            [clj-time.core :refer [date-time]]
            [migrate.connection :refer [with-connection]])
  (:use [clj-time.coerce :only (to-date-time)]
        clojure.test
        environ.core
        migrate.core
        migrate.test))

(deftest test-latest-migration
  (let [ns 'migrate.example]
    (is (= (latest-migration ns) (last (find-migrations ns))))))

(deftest test-latest-version
  (is (= (date-time 2012 8 17 14 29) (latest-version 'migrate.example))))

(deftest test-find-migration-by-version
  (let [ns 'migrate.example]
    (are [version expected]
      (is (= (require-migration expected) (find-migration-by-version ns version)))
      (date-time 2012 8 17 14 26)
      'migrate.example.20120817142600-create-continents
      (date-time 2012 8 17 14 28)
      'migrate.example.20120817142800-create-countries
      (date-time 2012 8 17 14 29)
      'migrate.example.20120817142900-create-regions)))

(deftest test-format-time
  (is (re-matches #"Thu, 01 Jan 1970 00:00:00 \+...." (format-time (java.util.Date. 0)))))

(deftest test-find-migrations
  (let [migrations (find-migrations 'migrate.example)]
    (is (= 3 (count migrations)))
    (is (every? (partial instance? migrate.core.Migration) migrations))
    (is (= 'migrate.example.20120817142600-create-continents (:ns (first migrations))))
    (is (= 'migrate.example.20120817142900-create-regions (:ns (last migrations))))))

(dbtest test-create-migration-table
  (drop-migration-table)
  (is (create-migration-table)))

(dbtest test-drop-migration-table
  (is (drop-migration-table)))

(dbtest test-migration-table?
  (is (migration-table?))
  (drop-migration-table)
  (is (not (migration-table?))))

(dbtest test-insert-migration
  (doseq [migration (find-migrations 'migrate.example)]
    (insert-migration migration)
    (is (= (select-current-version) (:version migration)))))

(dbtest test-delete-migration
  (let [migrations (find-migrations 'migrate.example)]
    (doall (map insert-migration migrations))
    (doseq [migration (reverse migrations)]
      (is (= (select-current-version) (:version migration)))
      (delete-migration migration)
      (is (not (= (select-current-version) (:version migration)))))
    (is (nil? (select-current-version)))))

(dbtest test-find-applicable-migrations
  (let [versions (map (fn [v] {:version v}) (map to-date-time ["2010-11-01T21:30:10" "2010-11-02T14:12:45" "2010-11-03T20:11:01"]))]
    (are [from to expected]
      (is (= (map to-date-time expected)
             (map :version (find-applicable-migrations versions from to))))
      nil "2010-11-01T21:30:10"
      ["2010-11-01T21:30:10"]
      nil "2010-11-02T14:12:45"
      ["2010-11-01T21:30:10" "2010-11-02T14:12:45"]
      nil "2010-11-03T20:11:01"
      ["2010-11-01T21:30:10" "2010-11-02T14:12:45" "2010-11-03T20:11:01"]
      "2010-11-01T21:30:10" "2010-11-02T14:12:45"
      ["2010-11-02T14:12:45"]
      "2010-11-02T14:12:45" "2010-11-01T21:30:10"
      ["2010-11-02T14:12:45"]
      "2010-11-03T20:11:01" nil
      ["2010-11-03T20:11:01" "2010-11-02T14:12:45" "2010-11-01T21:30:10"]
      "2010-11-03T20:11:01" "2010-11-01T21:30:10"
      ["2010-11-03T20:11:01" "2010-11-02T14:12:45"]
      "2010-11-03T20:11:01" "2010-11-02T14:12:45"
      ["2010-11-03T20:11:01"])))

(deftest test-require-migration
  (let [migration (require-migration 'migrate.example.20120817142600-create-continents)]
    (is (= 'migrate.example.20120817142600-create-continents (:ns migration)))
    (is (= (ns-resolve 'migrate.example.20120817142600-create-continents 'up) (:up migration)))
    (is (= (ns-resolve 'migrate.example.20120817142600-create-continents 'down) (:down migration)))
    (is (= (date-time 2012 8 17 14 26) (:version migration)))))

(dbtest test-run-up
  (let [migrations (find-migrations 'migrate.example)]
    (is (run-up (nth migrations 0)))
    (is (= (date-time 2012 8 17 14 26) (select-current-version)))
    (is (run-up (nth migrations 1)))
    (is (= (date-time 2012 8 17 14 28) (select-current-version)))
    (is (run-up (nth migrations 2)))
    (is (= (date-time 2012 8 17 14 29) (select-current-version)))))

(dbtest test-run-down
  (let [migrations (find-migrations 'migrate.example)]
    (doall (map run-up migrations))
    (is (= (date-time 2012 8 17 14 29) (select-current-version)))
    (is (run-down (nth migrations 2)))
    (is (= (date-time 2012 8 17 14 28) (select-current-version)))
    (is (run-down (nth migrations 1)))
    (is (= (date-time 2012 8 17 14 26) (select-current-version)))
    (is (run-down (nth migrations 0)))
    (is (nil? (select-current-version)))))

(dbtest test-run-all-up
  (run 'migrate.example)
  (is (= (select-current-version) (:version (latest-migration 'migrate.example)))))

(dbtest test-run-up-to
  (doseq [migration (find-migrations 'migrate.example)]
    (run 'migrate.example (:version migration))
    (is (= (:version migration) (select-current-version)))))

(dbtest test-run-down-to
  (let [ns 'migrate.example]
    (run ns)
    (doseq [migration (reverse (find-migrations ns))]
      (run ns (:version migration))
      (is (= (:version migration) (select-current-version))))))

(dbtest test-run-all-down
  (let [ns 'migrate.example]
    (run ns)
    (is (= (:version (latest-migration ns)) (select-current-version)))
    (run ns 0)
    (is (nil? (select-current-version)))))

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
  (is (nil? (select-migrations))))
