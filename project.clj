(defproject feedletter "0.2.0"
  :description "Send Atom and RSS feed updates via email"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/data.zip "0.1.1"]
                 [com.draines/postal "1.11.3"]
                 [org.clojure/tools.logging "0.3.1"]
                 [ch.qos.logback/logback-classic "1.1.3"]
                 [hiccup "1.0.5"]]
  :main feedletter.core
  :uberjar-name "feedletter.jar")
