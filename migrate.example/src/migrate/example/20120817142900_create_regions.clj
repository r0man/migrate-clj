(ns migrate.example.20120817142900-create-regions
  (:require [clojure.java.jdbc :as jdbc]))

(defn up
  "Create regions table."
  [] (jdbc/create-table
      :regions
      [:id :text "PRIMARY KEY"]
      [:country_id :text]))

(defn down
  "Drop regions table."
  [] (jdbc/drop-table :regions))
