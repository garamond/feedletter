(ns feedletter.core
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [clojure.xml :as xml]
            [clojure.zip :as zip]
            [clojure.edn :as edn]
            [clojure.data.zip.xml :as z]
            [clojure.tools.logging :as log]
            [postal.core :as p])
  (:gen-class))

(defn read-res [res]
  (-> res
      slurp
      edn/read-string))

(def ^:dynamic *state-dir* (io/file ".state"))

(defn state-token [feed-name]
  (io/file *state-dir* (str feed-name ".edn")))

(defn read-state [feed-name]
  (try
    (read-res (state-token feed-name))
    (catch Exception e #{})))

(defn write-state [feed-name m]
  (do
    (.mkdirs *state-dir*)
    (spit (state-token feed-name) (prn-str m))))

(defn zip-feed [url]
  (zip/xml-zip (xml/parse url)))

(defn parse-feed [feed-url]
  (let [feed (zip-feed feed-url)]
    {:title (or (z/xml1-> feed :title z/text)          ;; Atom
                (z/xml1-> feed :channel :title z/text) ;; RSS
                )
     :entries
     (for [e (or (seq (z/xml-> feed :entry))          ;; Atom
                 (seq (z/xml-> feed :channel :item))) ;; RSS
           ]

       {:title (z/xml1-> e :title z/text)
        :date (or (z/xml1-> e :published z/text) ;; Atom
                  (z/xml1-> e :pubDate z/text)) ;; RSS
        :link (or (z/xml1-> e :link (z/attr :href)) ;; Atom
                  (z/xml1-> e :link z/text))} ;; RSS
       )}))

(defn gen-id [entry]
  (s/join " " (mapv entry [:date :title])))

(defn process-feed [m]
  (let [state (read-state (:title m))
        m' (update-in m [:entries] (fn [ex] (remove #(some #{(gen-id %)} state) ex)))]
    (write-state (:title m) (apply conj state (map gen-id (:entries m'))))
    m'
    ))

(defn make-body [entry]
  (str (:title entry) "\n" (:date entry) "\n" (:link entry) "\n\n"))

(defn make-message [cfg m]
  {:from (or (:from cfg) "feedletter")
   :to (:to cfg)
   :subject (s/join " " [(or (:subject cfg) "Feed update:") (:title m) "-" (count (:entries m)) "new items"])
   :body (apply str (map make-body (:entries m)))})

(defn -main [& args]
  (let [cfg (read-res (first args))]
    (doseq [f (:feeds cfg)]
      (log/debug "processing" f)
      (->> f
           parse-feed
           process-feed
           (make-message cfg)
           (#(when (seq (:body %))
              (p/send-message %)))
           ))))
