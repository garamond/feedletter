(defproject feedletter "0.4.1-SNAPSHOT"
  :description "Send Atom and RSS feed updates via email"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojars.scsibug/feedparser-clj "0.4.0"]
                 [com.draines/postal "1.11.4"]
                 [com.taoensso/timbre "4.2.1"]
                 [hiccup "1.0.5"]]
  :main feedletter.core
  :uberjar-name "feedletter.jar"
  :aot :all)
