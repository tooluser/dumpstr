(ns dumpstr.test.core.handler-test
  (:use midje.sweet)
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [dumpstr.core.util :as util]
            [dumpstr.core.handler :refer :all]))

(facts "About root-level pages"
       (fact "Nonsense is served at root"
             (let [{:keys [status body]} (app (mock/request :get "/"))]
               status => 200
               body => #"l¡ttr"))

       (fact "Proper version is returned"
             (let [response (app (mock/request :get "/info"))]
               (response :status) => 200
               (response :body) =>
               (contains (str "API version: " util/project-version)))))

(facts
 "About authentication"
 (fact "Show user is for admins only"
       (let [response (app (mock/request :get "/show-user/bofh"))]
         (:status response) => 200)))





