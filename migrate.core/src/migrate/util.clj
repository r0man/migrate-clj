(ns migrate.util
  (:require [clj-time.core :refer [date-time]]
            [clojure.java.classpath :refer [classpath]]
            [clojure.tools.namespace.find :refer [find-namespaces]]
            [inflections.number :refer [parse-integer]]))

(defn parse-version
  "Parse the version timestamp from the namespace `ns`."
  [ns]
  (let [[year month day hour minute second]
        (map parse-integer (rest (re-matches #".*(\d{4})(\d{2})(\d{2})(\d{2})(\d{2})(\d{2}).*"(str ns))))]
    (if (and year month day hour minute second)
      (date-time year month day hour minute second))))

(defn re-ns-matches
  "Finds all namespaces on the classpath matching `re`."
  [re] (filter #(re-matches re (str %1)) (find-namespaces (classpath))))

(defn resolve-var
  "Resolve `var` in `ns` and return a map with :var and :doc keys. If
  `var` can't be found thrown an AssertionError."
  [ns var]
  (if-let [v (ns-resolve ns var)]
    {:var v :doc (:doc (meta v))}
    (throw (AssertionError. (format "Can't resolve var #'%s in ns %s." var ns)))))
