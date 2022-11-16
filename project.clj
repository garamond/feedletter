(defproject feedletter "0.4.4-SNAPSHOT"
  :description "Send Atom and RSS feed updates via email"
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojars.scsibug/feedparser-clj "0.4.0"]
                 [com.draines/postal "2.0.5"]
                 [com.taoensso/timbre "6.0.1"]
                 [hiccup "1.0.5"]]
  :main feedletter.core
  :uberjar-name "feedletter.jar"
  :aot :all)
