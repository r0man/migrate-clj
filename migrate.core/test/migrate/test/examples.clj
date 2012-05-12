(ns migrate.test.examples
  (:require [clojure.java.jdbc :as jdbc])
  (:use migrate.core))

(defmigration "2010-11-01T21:30:10"
  "Create continent table."
  (jdbc/create-table
   "continents"
   [:id :text "PRIMARY KEY"])
  (jdbc/drop-table "continents"))

(defmigration "2010-11-02T14:12:45"
  "Create country table."
  (jdbc/create-table
   "countries"
   [:id :text "PRIMARY KEY"]
   [:continent_id :text])
  (jdbc/drop-table "countries"))

(defmigration "2010-11-03T20:11:01"
  "Create region table."
  (jdbc/create-table
   "regions"
   [:id :text "PRIMARY KEY"]
   [:country_id :text])
  (jdbc/drop-table "regions"))
