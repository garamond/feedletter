(ns feedletter.core
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [clojure.edn :as edn]
            [taoensso.timbre :refer [info debug error]]
            [postal.core :as p]
            [hiccup.core :as h]
            [feedparser-clj.core :as fp])
  (:gen-class))

(defn read-rsrc [rsrc]
  (-> rsrc
      slurp
      edn/read-string))

(def state-dir (io/file ".state"))

(defn state-token [feed-title]
  (io/file state-dir (str (s/escape feed-title {\/ " "}) ".edn")))

(defn read-state [feed-title]
  (try
    (read-rsrc (state-token feed-title))
    (catch Exception e {})))

(defn write-state! [feed-title s]
  (spit (state-token feed-title) (prn-str s)))

(defn feed-title [feed]
  (if (empty? (:title feed))
    (assoc feed :title (-> (java.net.URI. (:link feed)) .getHost))
    feed))

(defn entry-date [entry]
  (or (:updated-date entry) (:published-date entry)))

(defn update-entries [feed]
  (let [state (read-state (:title feed))
        new-entries (remove #(some #{(:title %)} (keys state)) (:entries feed))]
    (write-state! (:title feed)
                  (reduce merge state
                          (map #(hash-map (:title %)
                                          {:link (:link %)
                                           :date (entry-date %)}) new-entries)))
    (assoc feed :entries new-entries)))

(defn msg-html [cfg feed]
  (h/html
    (interpose [:div {:style "clear:both;"} [:hr]]
               (for [entry (:entries feed)]
                 [:div
                  [:h2 [:a {:href (:link entry)} (:title entry)]]
                  (if-let [contents (or (-> entry :contents first :value)
                                        (-> entry :description :value))]
                    [:p (if (:filter-imgs cfg) (s/replace contents #"<img.*>" "") contents)])
                  (if-let [date (entry-date entry)]
                    [:p (str date)])]))))

(defn make-msg [cfg feed]
  {:from    (format "\"%s\" <%s>" (:title feed) (:from cfg))
   :to      (:to cfg)
   :subject (format "News feed: %s new items" (count (:entries feed)))
   :body    [{:type    "text/html; charset=utf-8"
              :content (msg-html cfg feed)}]})

(defn send-msg [cfg msg]
  (when (seq (:content (first (:body msg))))
    (info "sending feedletter:" (:subject msg))
    (p/send-message (or (:smtp cfg) {}) msg)))

(defn -main [& args]
  (let [cfg (read-rsrc (if (seq args) (first args) "config.edn"))]
    (doseq [url (:feeds cfg)]
      (try
        (info "processing" url)
        (.mkdirs state-dir)
        (let [input-stream (-> (java.net.URL. url)
                               .openConnection
                               (doto (.setRequestProperty "User-Agent"
                                                          "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11"))
                               .getContent)]
        (->> input-stream
             fp/parse-feed
             feed-title
             update-entries
             (make-msg cfg)
             (send-msg cfg)))
        (catch Exception e (.printStackTrace e))))
    (info "finished")))
