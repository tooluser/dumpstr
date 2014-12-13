(defproject dumpstr "0.1.0"
  :description "FIXME: write description"
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
                 [com.amazonaws/aws-java-sdk "1.8.6"
                  :exclusions [joda-time]]
                 ]
  :plugins [[lein-ring "0.8.13"]]
  :ring {:handler dumpstr.core.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]]}})