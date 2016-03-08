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

(defn msg-html [feed]
  (h/html
    (interpose [:div {:style "clear:both;"} [:hr]]
               (for [entry (:entries feed)]
                 [:div
                  [:h2 [:a {:href (:link entry)} (:title entry)]]
                  (if-let [contents (-> entry :contents first :value)]
                    [:p contents])
                  (if-let [date (entry-date entry)]
                    [:p (str date)])]))))

(defn make-msg [cfg feed]
  {:from    (s/join ["\""(:title feed) "\" <" (:from cfg) ">"])
   :to      (:to cfg)
   :subject (s/join [(:title feed) ": " (count (:entries feed)) " new items"])
   :body    [{:type    "text/html; charset=utf-8"
              :content (msg-html feed)}]})

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
        (->> url
             fp/parse-feed
             feed-title
             update-entries
             (make-msg cfg)
             (send-msg cfg))
        (catch Exception e (.printStackTrace e))))
    (info "finished")))
