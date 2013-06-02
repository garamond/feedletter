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

(def ^:dynamic *archive-dir* (io/file ".archive"))

(defn state-token [feed-name]
  (io/file *archive-dir* (str feed-name ".edn")))

(defn read-state [feed-name]
  (try
    (read-res (state-token feed-name))
    (catch Exception e {})))

(defn write-state! [feed-name s]
  (spit (state-token feed-name) (prn-str s)))

(defn zip-feed [url]
  (zip/xml-zip (xml/parse url)))

(defn parse-feed [feed-url]
  (let [feed (zip-feed feed-url)]
    {:name (or (z/xml1-> feed :title z/text)          ;; Atom
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
  (s/reverse (s/replace (str (mapv entry [:date :title])) #"\W" "")))

(defn archive-feed! [fd]
  (let [out-dir (io/file *archive-dir* (:name fd))]
    (.mkdirs out-dir)
    (doseq [e (:entries fd)]
      (log/debug "archiving" e)
      (spit (io/file out-dir (str (gen-id e) ".html"))
            (slurp (s/replace (:link e) " " "+")))))
  (:name fd))

(defn process-feed [fd]
  (let [state (read-state (:name fd))
        fd' (update-in fd [:entries] (fn [ex] (remove #(some #{(gen-id %)} (keys state)) ex)))]
    (write-state! (:name fd) (reduce merge state (map #(hash-map (gen-id %) %) (:entries fd'))))
    fd'
    ))

(defn entry-str [entry]
  (str (:title entry) "\n" (:date entry) "\n" (:link entry) "\n\n"))

(defn make-msg [cfg fd]
  (with-meta {:from (or (:from cfg) "feedletter")
              :to (:to cfg)
              :subject (s/join " " [(or (:subject cfg) "Feed update:") (:name fd) "-" (count (:entries fd)) "new items"])
              :body (apply str (map entry-str (:entries fd)))}
    (or (:smtp cfg) {})))

(defn send-msg [msg]
  (when (seq (:body msg))
    (log/debug "sending feedletter" (:subject msg))
    (p/send-message msg)))

(defn err [s]
  (log/error s))

(defn -main [& args]
  (let [cfg (read-res (first args))
        a (agent [])]
    (set-error-handler! a err)
    (doseq [f (:feeds cfg)]
      (log/debug "processing" f)
      (.mkdirs *archive-dir*)
      (cond->> (or (:url f) f)
               true parse-feed
               true process-feed
               (:archive f) (#(do (send a (fn [s] conj s (archive-feed! %))) %))
               true (make-msg cfg)
               true send-msg
               ))
    (await a)
    (shutdown-agents)
    (log/debug "finished")))
