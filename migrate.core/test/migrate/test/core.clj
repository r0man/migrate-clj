(ns migrate.test.core
  (:import [java.sql DriverManager SQLException])
  (:require [clojure.java.jdbc :as sql]
            [clj-time.core :refer [date-time]])
  (:use [clj-time.coerce :only (to-date-time)]
        clojure.test
        environ.core
        migrate.core
        migrate.test.examples))

(def ^:dynamic *database*
  {:classname "org.postgresql.Driver"
   :subprotocol "postgresql"
   :subname "//localhost/migrate_test"
   :user (env :user)
   :password ""})

(defn cleanup-db []
  (doseq [table ["regions" "countries" "continents" "schema_migrations"]]
    (try (sql/do-commands (str "DROP TABLE IF EXISTS " table))
         (catch Exception _ nil))))

(defmacro dbtest [name & body]
  `(deftest ~name
     (sql/with-connection *database*
       (cleanup-db)
       ~@body)))

(defmacro with-version-table [& body]
  `(try (do (create-migration-table) ~@body)
        (finally (drop-migration-table))))

(deftest test-latest-migration
  (is (= (latest-migration) (find-migration-by-version "2010-11-03T20:11:01"))))

(deftest test-latest-version
  (is (= (to-date-time "2010-11-03T20:11:01") (latest-version))))

(deftest test-find-migration-by-versions
  (is (thrown? Exception (find-migration-by-version "invalid version")))
  (are [version]
    (is (= (to-date-time version) (:version (find-migration-by-version version))))
    "2010-11-01T21:30:10"
    "2010-11-02T14:12:45"
    "2010-11-03T20:11:01"))

(deftest test-format-time
  (is (re-matches #"1970-01-01T0.:00:00Z" (format-time (java.util.Date. 0)))))

(deftest test-find-migrations
  (let [migrations (find-migrations 'migrate.example)]
    (is (= 3 (count migrations)))
    (is (every? (partial instance? migrate.core.Migration) migrations))
    (is (= 'migrate.example.20120817142600-create-continents (:ns (first migrations))))
    (is (= 'migrate.example.20120817142900-create-regions (:ns (last migrations))))))

(dbtest test-create-migration-table
  (is (create-migration-table))
  (drop-migration-table))

(dbtest test-drop-migration-table
  (create-migration-table)
  (is (drop-migration-table)))

(dbtest test-migration-table?
  (is (not (migration-table?)))
  (create-migration-table)
  (is (migration-table?))
  (drop-migration-table)
  (is (not (migration-table?))))

(dbtest test-insert-migration
  (with-version-table
    (doseq [version ["2010-11-01T21:30:10" "2010-11-02T14:12:45"]]
      (let [migration (find-migration-by-version version)]
        (insert-migration migration)
        (is (= (select-current-version) (:version migration)))))))

(dbtest test-delete-migration
  (with-version-table
    (let [migrations (map find-migration-by-version ["2010-11-01T21:30:10" "2010-11-02T14:12:45"])]
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
    (is (= #'migrate.example.20120817142600-create-continents/up (:up migration)))
    (is (= #'migrate.example.20120817142600-create-continents/down (:down migration)))
    (is (= (date-time 2012 8 17 14 26) (:version migration)))))

(dbtest test-run-all-up
  (run)
  (is (= (select-current-version) (:version (latest-migration)))))

(dbtest test-run-up-to
  (doseq [version (sort (map :version (vals @*migrations*)))]
    (run version)
    (is (= (select-current-version) version))))

(dbtest test-run-down-to
  (run)
  (doseq [version (reverse (sort (map :version (vals @*migrations*))))]
    (run version)
    (is (= (select-current-version) version))))

(dbtest test-run-all-down
  (run)
  (is (= (select-current-version) (:version (latest-migration))))
  (run 0)
  (is (nil? (select-current-version))))

(dbtest test-select-current-version
  (is (thrown? SQLException (select-current-version)))
  (with-version-table
    (is (nil? (select-current-version)))
    (insert-migration (find-migration-by-version "2010-11-01T21:30:10"))
    (is (= (to-date-time "2010-11-01T21:30:10") (select-current-version)))
    (insert-migration (find-migration-by-version "2010-11-02T14:12:45"))
    (is (= (to-date-time "2010-11-02T14:12:45") (select-current-version)))))

(dbtest test-select-migrations
  (is (nil? (select-migrations)))
  (with-version-table
    (is (= [] (select-migrations)))
    (insert-migration (find-migration-by-version "2010-11-01T21:30:10"))
    (is (= [(to-date-time "2010-11-01T21:30:10")] (map :version (select-migrations))))))
