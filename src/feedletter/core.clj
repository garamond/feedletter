(ns feedletter.core
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [clojure.xml :as xml]
            [clojure.zip :as zip]
            [clojure.edn :as edn]
            [clojure.data.zip.xml :as z]
            [clojure.tools.logging :as log]
            [postal.core :as p]
            [hiccup.core :as h])
  (:gen-class))

(defn read-rsrc [rsrc]
  (-> rsrc
      slurp
      edn/read-string))

(def state-dir (io/file ".state"))

(defn state-token [feed-name]
  (io/file state-dir (str feed-name ".edn")))

(defn read-state [feed-name]
  (try
    (read-rsrc (state-token feed-name))
    (catch Exception e #{})))

(defn write-state! [feed-name s]
  (spit (state-token feed-name) (prn-str s)))

(defn zip-feed [url]
  (try
    (zip/xml-zip (xml/parse url))
    (catch Exception e (log/error (.getMessage e)))))

(defn escape-str [s]
  (s/escape s {\/ " "}))

(defn parse-feed [url]
  (let [feed (zip-feed url)]
    {:name (or (z/xml1-> feed :title z/text)           ;; Atom
               (z/xml1-> feed :channel :title z/text)  ;; RSS
               )
     :entries
     (for [e (or (seq (z/xml-> feed :entry))          ;; Atom
                 (seq (z/xml-> feed :channel :item))) ;; RSS
           ]
       {:title (z/xml1-> e :title z/text)
        :link (or (z/xml1-> e :link (z/attr :href))   ;; Atom
                  (z/xml1-> e :link z/text))          ;; RSS
        :desc (or (z/xml1-> e :content z/text)        ;; Atom
                  (z/xml1-> e :description z/text))}  ;; RSS
       )}))

(defn process-feed [fd]
  (let [state (read-state (:name fd))
        fd' (update-in fd [:entries] (fn [ex] (remove #(some #{(escape-str (:title %))} state) ex)))]
    (write-state! (escape-str (:name fd))
                  (reduce merge state (for [e (:entries fd')]
                                        (escape-str (:title e)))))
    fd'))

(defn msg-html [entries]
  (h/html
   (interpose [:div {:style "clear:both;"} [:hr]]
              (for [entry entries]
                [:div [:h2 [:a {:href (:link entry)} (:title entry)]] (if-let [desc (:desc entry)] [:p desc])]))))

(defn make-msg [cfg fd]
  {:from (s/join [(:name fd) " <" (:from cfg) ">"])
   :to (:to cfg)
   :subject (s/join [(:name fd) " feed: " (count (:entries fd)) " new items"])
   :body [{:type "text/html; charset=utf-8"
           :content (msg-html (:entries fd))}]})

(defn send-msg [cfg msg]
  (when (seq (:content (first (:body msg))))
    (log/debug "sending feedletter" (:subject msg))
    (p/send-message (or (:smtp cfg) {}) msg)))

(defn err [s]
  (log/error s))

(defn -main [& args]
  (let [cfg (read-rsrc (first args))]
    (doseq [url (:feeds cfg)]
      (log/debug "processing" url)
      (.mkdirs state-dir)
      (->> url
           parse-feed
           process-feed
           (make-msg cfg)
           (send-msg cfg)
           ))
    (log/debug "finished")))
