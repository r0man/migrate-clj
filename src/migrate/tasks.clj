(ns migrate.tasks
  (:require [clojure.java.jdbc :as jdbc])
  (:use [leiningen.env.core :only (set-environments!)]
        migrate.core))

(defn load-migrations
  "Load the project's migration namespaces."
  [project]
  (doseq [ns (:migrate project)]
    (require ns)))

(defn print-migrations [project env]
  (load-migrations project)
  (set-environments! project)
  (with-connection
    (let [migrations (select-migrations)
          migrations (zipmap (map :version migrations) migrations)]
      (println "VERSION              STATUS   WHEN                 DESCRIPTION")
      (println "-----------------------------------------------------------------------------------------")
      (doseq [{:keys [description version]} (sort-by :version (vals @*migrations*))
              :let [migration (get migrations version)]]
        (-> (format
             "%-20s %-8s %-20s %s"
             version
             (if migration "DONE" "PENDING")
             (or (format-time (:created_at migration)) "-")
             description)
            (println))))))

(defn run-migrations [project & [version]]
  (load-migrations project)
  (set-environments! project)
  (with-connection (run version)))
