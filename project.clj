(defproject feedletter "0.2.0"
  :description "Send Atom and RSS feed updates via email"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/data.zip "0.1.1"]
                 [com.draines/postal "1.10.2"]
                 [org.clojure/tools.logging "0.2.6"]
                 [ch.qos.logback/logback-classic "1.0.13"]]
  :main feedletter.core
  :uberjar-name "feedletter.jar")
