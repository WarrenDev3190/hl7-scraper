(ns hl7-scraper.hl7-helpers
  (:require [net.cgrand.enlive-html :as html]
            [hl7-scraper.hl7-constants :as const]
            [hl7-scraper.hl7-regexer :as rex]))

(defmacro swallow-exceptions [& body]
  "Capture Exceptions and returns nil if exception met"
  `(try ~@body (catch Exception e#)))

(defn fetch-from-url [url]
  (->> url
       (java.net.URL.)
       (html/html-resource)
       (swallow-exceptions)))

(defn fetch-hl7-tree []
  (fetch-from-url const/hl7-key-tree))

(defn get-hl7-key-list []
  (->> (html/select (fetch-hl7-tree) [:li :li :li :li :a])
       (map #(:content %))
       (map #(first %))
       (map #(:content %))
       (map #(first %))))

(defn make-ver-list []
  (->> (range 1 7)
       (map #(str "v2" %))))


(defn transform-url [ver hl7-key]
  (-> const/base-url
    (clojure.string/replace (re-pattern "\\[\\w+\\]") ver)
    (clojure.string/replace (re-pattern "\\(\\w+\\)") hl7-key)))

(defn get-data [ver key]
  (let [raw (->> (html/select
                   (fetch-from-url
                     (transform-url ver key)) [:div.block :ul :li])
                 (map #(:content %))
                 (map #(first %))
                 (map #(rex/regex-it %))
                 (map #(first %)))]
    (assoc {} key
      (reduce
        (fn [obj row]
          (assoc obj (swallow-exceptions (parse-int (nth row 3)) ) {:type (nth row 4) :code (nth row 5)})) {} raw))))


(defn parse-int [s]
  (Integer. s))

(defn get-by-ver [ver]
  (let [ver-key (ver-to-float ver)]
    (assoc {} ver-key
      (apply merge
        (map #(get-data ver %)(get-hl7-key-list))))))

(defn ver-to-float [ver]
  (->> (re-matches  (re-pattern "v(\\d)(\\d)") ver)
       (rest)
       (clojure.string/join ".")
       (Float.)))

(defn retrieve-hl7 []
  (apply merge (map #(get-by-ver %)(make-ver-list))))



