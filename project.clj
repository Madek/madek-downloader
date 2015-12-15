(defproject madek-downloader "0.1.0"
  :description "Madek Downloader"
  :url "https://github.com/Madek/madek-downloader"
  :license {:name "GPL v3"}
  :dependencies [
                 [environ "1.0.1"]
                 [json-roa_clj-client "0.1.0"]
                 [log4j/log4j "1.2.17" :exclusions [javax.mail/mail javax.jms/jms com.sun.jdmk/jmxtools com.sun.jmx/jmxri]]
                 [logbug "2.0.0-beta.8"]
                 [org.clojure/clojure "1.7.0"]
                 [org.clojure/tools.cli "0.3.3"]
                 [org.slf4j/slf4j-log4j12 "1.7.12"]
                 ]
  :aot [madek.downloader.main]
  :main madek.downloader.main
  )
