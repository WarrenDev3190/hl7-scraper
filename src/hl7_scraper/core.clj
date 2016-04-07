(ns hl7-scraper.core
  (:require [net.cgrand.enlive-html :as html]))

(def ^:const hl7-key-tree
  "http://hl7api.sourceforge.net/v21/apidocs/ca/uhn/hl7v2/model/v21/segment/package-tree.html")

(def ^:const base-url
  "http://hl7api.sourceforge.net/v21/apidocs/ca/uhn/hl7v2/model/v21/segment/ADT.html")

(def matcher #"((\w{3})-(\d\d?)):\s(.+)\s\(([A-Za-z0-9_]+)\)")

(defn regex-it [str]
  (re-seq matcher str))

(defn- splice
  [x] (str "$1" x "$2"))

(defn version-replacer [version url]
  (clojure.string/replace url #"(\/v\d)\d(\/)" (splice version)))

(defn segment-replacer [segment url]
  (clojure.string/replace url #"(\/)[A-Z]{3}(.html)" (splice segment)))

(defn fetch-from-url [url]
  (-> url
      java.net.URL.
      html/html-resource))

(defn get-hl7-key-list [ver]
  (->> (html/select (-> (version-replacer ver hl7-key-tree) fetch-from-url)
                    [:li :li :li :li :a])
       (map :content)
       (map first)
       (map :content)
       (map first)))

(defn get-data [ver key]
  (let [raw (->> (html/select
                   (fetch-from-url
                     ((comp #(let [_ (println (str "Fetching from url: " %))] %)
                            (partial version-replacer ver)
                            (partial segment-replacer key))
                       base-url))
                   [:div.block :ul :li])
                 (map :content)
                 (map first)
                 (map regex-it)
                 (map first))]
    (assoc {} key
              (reduce
                (fn [obj row]
                  (println row)
                  (assoc obj (Integer. (nth row 3)) {:type (nth row 4) :code (nth row 5)})) {} raw))))

(defn get-by-ver [ver]
  (let [ver-key ((comp float #(+ 2 (/ % 10))) ver)]
    (assoc {} ver-key
              (apply merge
                     (map (partial get-data ver)(get-hl7-key-list ver))))))


(defn retrieve-hl7 []
  (apply merge (map #(get-by-ver %) (range 1 7))))

(defn -main []
  (spit "hl7.edn"
        (pr-str (retrieve-hl7))))