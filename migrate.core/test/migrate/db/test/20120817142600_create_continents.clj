(ns migrate.db.test.20120817142600-create-continents
  (:require [clojure.java.jdbc :as jdbc]))

(defn up
  "Create continents table."
  [] (jdbc/create-table
      "continents"
      [:id :text "PRIMARY KEY"]))

(defn down
  "Drop continents table."
  [] (jdbc/drop-table "continents"))
