(defproject feedletter "0.1.0-SNAPSHOT"
  :description "Send Atom and RSS feed updates via email"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/data.zip "0.1.1"]
                 [com.draines/postal "1.10.2"]]
  :main feedletter.core
  :uberjar-name "feedletter.jar")
