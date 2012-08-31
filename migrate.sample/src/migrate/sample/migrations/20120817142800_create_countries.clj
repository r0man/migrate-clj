(ns migrate.sample.migrations.20120817142800-create-countries
  (:require [clojure.java.jdbc :as jdbc]))

(defn up
  "Create countries table."
  [] (jdbc/create-table
      :countries
      [:id :text "PRIMARY KEY"]
      [:continent_id :text]))

(defn down
  "Create countries table."
  [] (jdbc/drop-table :countries))
