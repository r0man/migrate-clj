(ns migrate.task.new
  (:refer-clojure :exclude [replace])
  (:import java.io.File)
  (:require [clj-time.core :refer [now date-time]]
            [clojure.java.io :refer [make-parents]]
            [clojure.string :refer [join replace]]
            [inflections.core :refer [capitalize]]
            [migrate.util :refer [format-time]]))

(defn template-ns
  "Returns the new template namespace."
  [base-ns created-at description]
  (symbol (str base-ns "." (format-time created-at) "-" (replace description #"(?i)[^A-Z]" "-"))))

(defn template-filename
  "Returns the new template filename."
  ([base-ns created-at description]
     (-> (template-ns base-ns created-at description)
         (replace #"\." File/separator)
         (replace #"-" "_")
         (str ".clj")))
  ([directory base-ns created-at description]
     (str directory File/separator (template-filename base-ns created-at description))))

(defn template
  "Returns the new template filename."
  [base-ns created-at description]
  (->> [(format "(ns %s\n  \"%s\")"
                (template-ns base-ns created-at description)
                (replace (capitalize description) #"\.*$" "."))
        "(defn up []\n  )"
        "(defn down []\n  )"]
       (join "\n\n")))

(defn create-template
  "Create a new migration template."
  [directory base-ns description & [created-at]]
  (let [created-at (or created-at (now))
        filename (template-filename directory base-ns created-at description)]
    (make-parents filename)
    (spit filename (template base-ns created-at description))
    filename))

;; (defn new [project & args]
;;   (create-template
;;    (first (:source-paths project))
;;    (:migrate-ns project)
;;    (apply str args)))
