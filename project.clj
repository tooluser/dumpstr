(defproject dumpstr "0.1.0-SNAPSHOT"
  :description "Littr backend"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[com.taoensso/faraday "1.5.0"
                  :exclusions [org.clojure/clojure]]
                 [org.clojure/clojure "1.6.0"]
                 [compojure "1.3.1"]
                 [ring/ring-defaults "0.1.2"]
                 [com.cemerick/friend "0.2.1"]
;;                 [com.cemerick/hiccup "1.0.5"];;
                 [hiccup "1.0.5"]
                 [cheshire "5.4.0"]
                 [com.amazonaws/aws-java-sdk "1.8.6"
                  :exclusions [joda-time]]
                 ]
  :plugins [[lein-ring "0.8.13"]
            [lein-midje "3.1.3"]]
  :ring {:handler dumpstr.core.handler/app}
  :profiles
  {:dev {:dependencies [[midje "1.6.3" :exclusions [org.clojure/clojure]]
                        [me.raynes/conch "0.8.0"]
                        [javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]]
         :resource-paths ["resources/test/dynamodb"]}})
