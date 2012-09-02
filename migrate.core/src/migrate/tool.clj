(ns migrate.tool
  (:gen-class)
  (:require [clojure.string :refer [blank?]]
            [commandline.core :refer [print-help with-commandline]]
            [environ.core :refer [env]]
            [migrate.core :refer [run]]
            [migrate.sql :refer [with-connection]]
            [migrate.task.new :refer [new]]
            [migrate.task.status :refer [status]]))

(defn- help-exit []
  (print-help "migrate [new|status|run] [OPTIONS]")
  (System/exit 1))

(defn- resolve-db-spec
  "Reolve `db-spec` via environ or return `db-spec`."
  [db-spec]
  (cond
   (keyword? db-spec)
   (env db-spec)
   (and (string? db-spec) (= \: (first db-spec)))
   (env (keyword (apply str (rest db-spec))))
   :else db-spec))

(defn -main [& [command & args]]
  (with-commandline [args args]
    [[d database "Run migrations in the database specified by DB." :string "DB"]
     [D directory "Create new migrations in the directory DIR (default: src)." :string "DIR"]
     [h help "Print this help"]
     [n namespace "Run the migrations in the namespace NS." :string "NS"]
     [v version "Run all migration up/down to VERSION." :string "VERSION"]]
    (when (or (blank? command)
              (blank? database)
              (blank? namespace))
      (help))
    (with-connection (resolve-db-spec database)
      (condp = command
        "new" (apply new (or directory "src") namespace args)
        "status" (status namespace)
        "run" (run namespace version)
        :else (help)))))

(defn -main [& args]
  (with-commandline [args args]
    [[d database "Run migrations in the database specified by DB." :string "DB"]
     [D directory "Create new migrations in the directory DIR (default: src)." :string "DIR"]
     [h help "Print this help"]
     [n namespace "Run the migrations in the namespace NS." :string "NS"]
     [v version "Run all migration up/down to VERSION." :string "VERSION"]]
    (when (or help
              (blank? (first args))
              (blank? database)
              (blank? namespace))
      (help-exit))
    (with-connection (resolve-db-spec database)
      (condp = (first args)
        "new" (apply new (or directory "src") namespace args)
        "status" (status namespace)
        "run" (run namespace version)
        :else (help-exit)))))
