(ns migrate.test.core
  (:import [java.sql DriverManager SQLException])
  (:require [clojure.java.jdbc :as sql])
  (:use [migrate.core] :reload)
  (:use [clojure.contrib.def :only (defvar)]
        clojure.test))

(defvar *database*
  {:classname "org.sqlite.JDBC",
   :subprotocol "sqlite",
   :subname "db/test.sqlite3"
   :create true}
  "The SQLite 3 database connection for the tests.")

(defvar *database*
  {:classname "org.postgresql.Driver"
   :subprotocol "postgresql"
   :subname "//localhost/migrate_test"
   :user "migrate"
   :password "migrate"}
  "The PostgreSQL database connection for the tests.")

(defmigration "2010-11-01 21:30:10"
  "Create continent table."
  (sql/create-table
   "continents"
   [:id :text "PRIMARY KEY"])
  (sql/drop-table "continents"))

(defmigration "2010-11-02 14:12:45"
  "Create country table."
  (sql/create-table
   "countries"
   [:id :text "PRIMARY KEY"]
   [:continent_id :text])
  (sql/drop-table "countries"))

(defmigration "2010-11-03 20:11:01"
  "Create region table."
  (sql/create-table
   "regions"
   [:id :text "PRIMARY KEY"]
   [:country_id :text])
  (sql/drop-table "regions"))

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
  (is (= (latest-migration) (find-migration-by-version "2010-11-03 20:11:01"))))

(deftest test-latest-version
  (is (= (latest-version) "2010-11-03 20:11:01")))

(deftest test-find-migration-by-versions
  (is (nil? (find-migration-by-version "unknown version")))
  (are [version]
    (is (= (:version (find-migration-by-version version)) version))
    "2010-11-01 21:30:10"
    "2010-11-02 14:12:45"
    "2010-11-03 20:11:01"))

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
    (doseq [version ["2010-11-01 21:30:10" "2010-11-02 14:12:45"]]
      (let [migration (find-migration-by-version version)]
        (insert-migration migration)
        (is (= (select-current-version) (:version migration)))))))

(dbtest test-delete-migration
  (with-version-table
    (let [migrations (map find-migration-by-version ["2010-11-01 21:30:10" "2010-11-02 14:12:45"])]
      (doall (map insert-migration migrations))
      (doseq [migration (reverse migrations)]
        (is (= (select-current-version) (:version migration)))
        (delete-migration migration)
        (is (not (= (select-current-version) (:version migration)))))
      (is (nil? (select-current-version))))))

(dbtest test-find-applicable-migrations
  (with-version-table
    (let [versions (map (fn [v] {:version v}) ["2010-11-01 21:30:10" "2010-11-02 14:12:45" "2010-11-03 20:11:01"])]
      (are [from to expected]
        (is (= (map :version (find-applicable-migrations versions from to)) expected))
        nil "2010-11-01 21:30:10"
        ["2010-11-01 21:30:10"]
        nil "2010-11-02 14:12:45"
        ["2010-11-01 21:30:10" "2010-11-02 14:12:45"]
        nil "2010-11-03 20:11:01"
        ["2010-11-01 21:30:10" "2010-11-02 14:12:45" "2010-11-03 20:11:01"]
        "2010-11-01 21:30:10" "2010-11-02 14:12:45"
        ["2010-11-02 14:12:45"]
        "2010-11-02 14:12:45" "2010-11-01 21:30:10"
        ["2010-11-02 14:12:45"]
        "2010-11-03 20:11:01" nil
        ["2010-11-03 20:11:01" "2010-11-02 14:12:45" "2010-11-01 21:30:10"]
        "2010-11-03 20:11:01" "2010-11-01 21:30:10"
        ["2010-11-03 20:11:01" "2010-11-02 14:12:45"]
        "2010-11-03 20:11:01" "2010-11-02 14:12:45"
        ["2010-11-03 20:11:01"]))))

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
    (insert-migration (find-migration-by-version "2010-11-01 21:30:10"))
    (is (= (select-current-version) "2010-11-01 21:30:10"))
    (insert-migration (find-migration-by-version "2010-11-02 14:12:45"))
    (is (= (select-current-version) "2010-11-02 14:12:45"))))
