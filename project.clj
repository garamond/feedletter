(defproject feedletter "0.2.0"
  :description "Send Atom and RSS feed updates via email"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/data.zip "0.1.1"]
                 [com.draines/postal "1.11.1"]
                 [org.clojure/tools.logging "0.3.0"]
                 [ch.qos.logback/logback-classic "1.1.2"]
                 [hiccup "1.0.5"]]
  :main feedletter.core
  :uberjar-name "feedletter.jar")
