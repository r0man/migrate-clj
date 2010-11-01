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
  `(try (do (create-version-table) ~@body) 
        (finally (drop-version-table))))

(defmigration "2010-11-01 21:30:10"
  "Create the continents table."      
  (sql/create-table
   "continents"
   [:id :string "PRIMARY KEY"])    
  (sql/drop-table :continents))

(defmigration "2010-11-01 21:32:45"
  "Create the countries table."    
  (sql/create-table
   "countries"
   [:id :string "PRIMARY KEY"]
   [:continent_id :string])    
  (sql/drop-table :continents))

(dbtest test-create-version-table
  (is (create-version-table))
  (drop-version-table))

(dbtest test-drop-version-table
  (create-version-table)
  (is (drop-version-table)))

(dbtest test-select-current-version
  (is (thrown? SQLException (select-current-version)))
  (with-version-table
    (is (nil? (select-current-version)))
    (sql/insert-records *version-table* {:id "201011012120"})
    (is (= (select-current-version) 201011012120))
    (sql/insert-records *version-table* {:id "201012012120"})
    (is (= (select-current-version) 201012012120))))

(dbtest test-version-table?
  (create-version-table)
  (is (version-table?))
  (drop-version-table)
  (is (not (version-table?))))

;; (dbtest test-find-applicable
;;   (is (thrown? Exception (find-applicable :invalid-direction)))
;;   (with-version-table
;;     (is (= (find-applicable "2010-11-01 21:30:10" "2010-11-01 21:30:10")
;;            []))
;;     (is (= (find-applicable "2010-11-01 21:30:10" "2010-11-01 21:32:45")
;;            ["2010-11-01 21:32:45" ]))
;;     (is (= (find-applicable "2010-11-01 21:32:45" "2010-11-01 21:30:10")
;;            ["2010-11-01 21:32:45" ]))
;;     (is (= (find-applicable "2010-11-01 21:30:09" "2010-11-01 21:32:45")
;;            ["2010-11-01 21:30:10" "2010-11-01 21:32:45"]))
;;     (is (= (find-applicable "2010-11-01 21:32:45" "2010-11-01 21:30:09")
;;            ["2010-11-01 21:32:45" "2010-11-01 21:30:10"]))))

(dbtest tet-run
  (with-version-table
    (run)))

;; (deftest replace-me
;;   (sql/with-connection *database*
;;     (sql/with-query-results result-set
;;       "SELECT * FROM test")))
