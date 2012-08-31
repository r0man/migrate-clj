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
  (is (re-matches #"1970-01-01T0.:00:00Z" (format-time (java.util.Date. 0)))))

(deftest test-find-migrations
  (let [migrations (find-migrations 'migrate.example)]
    (is (= 3 (count migrations)))
    (is (every? (partial instance? migrate.core.Migration) migrations))
    (is (= 'migrate.example.20120817142600-create-continents (:ns (first migrations))))
    (is (= 'migrate.example.20120817142900-create-regions (:ns (last migrations))))))

(dbtest test-create-migration-table
  (is (create-migration-table)))

(dbtest test-drop-migration-table
  (create-migration-table)
  (is (drop-migration-table)))

(dbtest test-migration-table?
  (create-migration-table)
  (is (migration-table?))
  (drop-migration-table)
  (is (not (migration-table?))))

(dbtest test-insert-migration
  (with-version-table
    (doseq [migration (find-migrations 'migrate.example)]
      (insert-migration migration)
      (is (= (select-current-version) (:version migration))))))

(dbtest test-delete-migration
  (with-version-table
    (let [migrations (find-migrations 'migrate.example)]
      (doall (map insert-migration migrations))
      (doseq [migration (reverse migrations)]
        (is (= (select-current-version) (:version migration)))
        (delete-migration migration)
        (is (not (= (select-current-version) (:version migration)))))
      (is (nil? (select-current-version))))))

(dbtest test-find-applicable-migrations
  (with-version-table
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
        ["2010-11-03T20:11:01"]))))

(deftest test-require-migration
  (let [migration (require-migration 'migrate.example.20120817142600-create-continents)]
    (is (= 'migrate.example.20120817142600-create-continents (:ns migration)))
    (is (= (ns-resolve 'migrate.example.20120817142600-create-continents 'up) (:up migration)))
    (is (= (ns-resolve 'migrate.example.20120817142600-create-continents 'down) (:down migration)))
    (is (= (date-time 2012 8 17 14 26) (:version migration)))))

(dbtest test-run-up
  (with-version-table
    (let [migrations (find-migrations 'migrate.example)]
      (is (run-up (nth migrations 0)))
      (is (= (date-time 2012 8 17 14 26) (select-current-version)))
      (is (run-up (nth migrations 1)))
      (is (= (date-time 2012 8 17 14 28) (select-current-version)))
      (is (run-up (nth migrations 2)))
      (is (= (date-time 2012 8 17 14 29) (select-current-version))))))

(dbtest test-run-down
  (with-version-table
    (let [migrations (find-migrations 'migrate.example)]
      (doall (map run-up migrations))
      (is (= (date-time 2012 8 17 14 29) (select-current-version)))
      (is (run-down (nth migrations 2)))
      (is (= (date-time 2012 8 17 14 28) (select-current-version)))
      (is (run-down (nth migrations 1)))
      (is (= (date-time 2012 8 17 14 26) (select-current-version)))
      (is (run-down (nth migrations 0)))
      (is (nil? (select-current-version))))))

;; (dbtest test-run-all-up
;;   (run)
;;   (is (= (select-current-version) (:version (latest-migration)))))

;; (dbtest test-run-up-to
;;   (doseq [version (sort (map :version (vals @*migrations*)))]
;;     (run version)
;;     (is (= (select-current-version) version))))

;; (dbtest test-run-down-to
;;   (run)
;;   (doseq [version (reverse (sort (map :version (vals @*migrations*))))]
;;     (run version)
;;     (is (= (select-current-version) version))))

;; (dbtest test-run-all-down
;;   (run)
;;   (is (= (select-current-version) (:version (latest-migration))))
;;   (run 0)
;;   (is (nil? (select-current-version))))

(dbtest test-select-current-version
  (with-version-table
    (is (nil? (select-current-version)))
    (insert-migration (nth (find-migrations 'migrate.example) 0))
    (is (= (date-time 2012 8 17 14 26) (select-current-version)))
    (insert-migration (nth (find-migrations 'migrate.example) 1))
    (is (= (date-time 2012 8 17 14 28) (select-current-version)))
    (insert-migration (nth (find-migrations 'migrate.example) 2))
    (is (= (date-time 2012 8 17 14 29) (select-current-version))))
  (is (thrown? SQLException (select-current-version))))

(dbtest test-select-migrations
  (with-version-table
    (is (= [] (select-migrations)))
    (doall (map insert-migration (find-migrations 'migrate.example)))
    (is (= [(date-time 2012 8 17 14 26)
            (date-time 2012 8 17 14 28)
            (date-time 2012 8 17 14 29)]
           (map :version (select-migrations)))))
  (is (nil? (select-migrations))))
