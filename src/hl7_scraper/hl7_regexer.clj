(ns hl7-scraper.hl7-regexer)


(def matcher
  (re-pattern "((\\w{3})-(\\d\\d?)):\\s(.+)\\s\\((\\w{2,3})\\)"))

(defn regex-it [str]
  (re-seq matcher str))
