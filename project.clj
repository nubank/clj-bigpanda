(defproject clj-bigpanda "0.0.1"
  :description "Clojure interface to the BigPanda service"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.logging "0.2.6"]
                 [clj-http "0.9.1"
                  :exclusions [commons-logging]]
                 [cheshire "5.2.0"]]
  :profiles
  {:dev {:dependencies [[log4j/log4j "1.2.16"
                         :exclusions [javax.mail/mail
                                      javax.jms/jms
                                      com.sun.jdmk/jmxtools
                                      com.sun.jmx/jmxri]]
                        [org.slf4j/slf4j-log4j12 "1.7.5"]
                        [org.slf4j/jcl-over-slf4j "1.7.5"]]}
   :test {:resource-paths ["resources" "test-resources"]}})
