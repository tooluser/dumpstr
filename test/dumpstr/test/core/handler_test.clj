(ns dumpstr.test.core.handler-test
  (:use midje.sweet)
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [dumpstr.core.handler :refer :all]))

(fact "Nonsense is served at root"
      (let [{:keys [status body]} (app (mock/request :get "/"))]
        status => 200
        body => #"l¡ttr"))

;; (fact "Proper version is returned"
;;       (let [response (app (mock/request :get "/info"))]
;;         (response :status) => 200))



