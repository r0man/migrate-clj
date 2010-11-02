(ns migrate.test.core
  (:import [java.sql DriverManager SQLException])
  (:require [clojure.contrib.sql :as sql])
  (:use [migrate.core] :reload)
  (:use [clojure.contrib.def :only (defvar)]
        clojure.test))

(defvar *database*
  {:classname "org.sqlite.JDBC",
   :subprotocol "sqlite",
   :subname "db/test.sqlite3"
   :create true}
  "The database connection for the tests.")

(defmacro dbtest [name & body]
  `(deftest ~name
     (.delete (java.io.File. (:subname *database*)))
     (sql/with-connection *database* ~@body)))

(defmacro with-version-table [& body]
  `(try (do (create-migration-table) ~@body) 
        (finally (drop-migration-table))))

(defmigration "2010-11-01 21:30:10"
  "Create continent table."      
  (sql/create-table
   "continents"
   [:id :string "PRIMARY KEY"])    
  (sql/drop-table :continents))

(defmigration "2010-11-01 21:32:45"
  "Create country table."    
  (sql/create-table
   "countries"
   [:id :string "PRIMARY KEY"]
   [:continent_id :string])    
  (sql/drop-table :continents))

(deftest test-latest-migration
  (is (= (latest-migration) (find-migration-by-version "2010-11-01 21:32:45"))))

(deftest test-latest-version
  (is (= (latest-version) "2010-11-01 21:32:45")))

(deftest test-find-migration-by-version
  (is (nil? (find-migration-by-version "unknown version")))
  (are [version]
    (is (= (:version (find-migration-by-version version)) version))
    "2010-11-01 21:30:10"
    "2010-11-01 21:32:45"))

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
    (doseq [version ["2010-11-01 21:30:10" "2010-11-01 21:32:45"]]
      (let [migration (find-migration-by-version version)]
        (insert-migration migration)
        (is (= (select-current-version) (:version migration)))))))

(dbtest test-delete-migration
  (with-version-table
    (let [migrations (map find-migration-by-version ["2010-11-01 21:30:10" "2010-11-01 21:32:45"])]
      (doall (map insert-migration migrations))
      (doseq [migration (reverse migrations)]
        (is (= (select-current-version) (:version migration)))
        (delete-migration migration)
        (is (not (= (select-current-version) (:version migration)))))
      (is (nil? (select-current-version))))))

(dbtest test-find-applicable-migrations
  (with-version-table
    (are [from to expected]
      (is (= (find-applicable-migrations from to)
             (map find-migration-by-version expected)))
      nil nil ["2010-11-01 21:30:10" "2010-11-01 21:32:45"]
      ""  ""  ["2010-11-01 21:30:10" "2010-11-01 21:32:45"]      
      nil "2010-11-01 21:32:45" ["2010-11-01 21:30:10" "2010-11-01 21:32:45"]
      "" "2010-11-01 21:32:45" ["2010-11-01 21:30:10" "2010-11-01 21:32:45"]
      "2010-11-01 21:30:10" "2010-11-01 21:32:45" ["2010-11-01 21:32:45"])))

(dbtest test-run  
  (run)
  (is (= (select-current-version) (:version (latest-migration)))))

(dbtest test-select-current-version
  (is (thrown? SQLException (select-current-version)))
  (with-version-table
    (is (nil? (select-current-version)))
    (sql/insert-records migration-table {:version "201011012120"})
    (is (= (select-current-version) 201011012120))
    (sql/insert-records migration-table {:version "201012012120"})
    (is (= (select-current-version) 201012012120))))
