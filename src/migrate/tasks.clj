(ns migrate.tasks
  (:require [clojure.java.jdbc :as jdbc])
  (:use [leiningen.env.core :only (environment set-environments! project-env variable-name)]
        migrate.core))

(defmacro with-connection [& body]
  `(jdbc/with-connection (:database (environment))
     ~@body))

(defn load-migrations [project]
  (doseq [ns (:migrate project)]
    (require ns)))

(defn print-migrations [project env]
  (load-migrations project)
  (set-environments! project)
  (with-connection
    (let [migrations (select-migrations)
          migrations (zipmap (map :version migrations) migrations)]
      (println "VERSION              STATUS   WHEN                   DESCRIPTION")
      (println "-----------------------------------------------------------------------------------------")
      (doseq [{:keys [description version]} (sort-by :version (vals @*migrations*))
              :let [migration (get migrations version)]]
        (-> (format
             "%-20s %-8s %-22s %s"
             version
             (if migration "DONE" "PENDING")
             (or (:created_at migration) "-")
             description)
            (println))))))

(defn run-migrations [project & [version]]
  (load-migrations project)
  (set-environments! project)
  (with-connection (run version)))
