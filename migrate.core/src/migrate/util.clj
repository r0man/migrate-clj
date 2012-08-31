(ns migrate.util
  (:require [environ.core :refer [env]]
            [clj-time.core :refer [date-time]]
            [clojure.string :refer [split]]
            [clojure.java.classpath :refer [classpath]]
            [clojure.tools.namespace.find :refer [find-namespaces]]
            [inflections.number :refer [parse-integer]]))

(def ^:dynamic *version-regex*
  #".*(\d{4})(\d{2})(\d{2})(\d{2})(\d{2})(\d{2}).*")

(defn parse-url
  "Parse `s` as an URI and return a Ring compatible map."
  [s]
  (if-let [matches (re-matches #"([^:]+)://(([^:]+):([^@]+)@)?(([^:/]+)(:([0-9]+))?((/[^?]*)(\?(.*))?))" s)]
    {:scheme (nth matches 1)
     :user (nth matches 3)
     :password (nth matches 4)
     :server-name (nth matches 6)
     :server-port (parse-integer (nth matches 8))
     :uri (nth matches 10)
     :query-string (nth matches 12)
     :params (->> (split (or (nth matches 12) "") #"&")
                  (map #(split %1 #"="))
                  (mapcat #(vector (keyword (first %1)) (second %1)))
                  (apply hash-map))}))

(defn parse-version
  "Parse the version timestamp from the namespace `ns`."
  [ns]
  (let [[year month day hour minute second]
        (map parse-integer (rest (re-matches *version-regex* (str ns))))]
    (if (and year month day hour minute second)
      (date-time year month day hour minute second))))

(defn re-ns-matches
  "Finds all namespaces on the classpath matching `re`."
  [re] (filter #(re-matches re (str %1)) (find-namespaces (classpath))))

(defn resolve-db-spec
  "Reolve `db-spec` via environ or return `db-spec`."
  [db-spec]
  (cond
   (keyword? db-spec)
   (env db-spec)
   (and (string? db-spec) (= \: (first db-spec)))
   (env (keyword (apply str (rest db-spec))))
   :else db-spec))

(defn resolve-var
  "Resolve `var` in `ns` and return a map with :var and :doc keys. If
  `var` can't be found thrown an AssertionError."
  [ns var]
  (if-let [v (ns-resolve ns var)]
    {:var v :doc (:doc (meta v))}
    (throw (AssertionError. (format "Can't resolve var #'%s in ns %s." var ns)))))
