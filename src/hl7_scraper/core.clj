(ns hl7-scraper.core
  (:require [hl7-scraper.hl7-helpers :as helpers]))

(defn -main []
  (spit "hl7.edn"
    (pr-str (helpers/retrieve-hl7))))