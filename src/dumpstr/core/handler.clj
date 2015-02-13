(ns dumpstr.core.handler
  (:require
   [compojure.core :refer :all]
   [compojure.route :as route]
   [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
   [ring.middleware.session :as sess]
   [ring.util.response :as resp]
   [ring.adapter.jetty :as jetty]
   [ring.component.jetty :refer [jetty-server]]
   [com.stuartsierra.component :as component]
   [hiccup.core :as h]
   [hiccup.element :as e]
   [cemerick.friend :as friend]
   (cemerick.friend [workflows :as workflows]
                    [credentials :as creds])
   [cheshire.core :as json]
   [dumpstr.core.util :as util]
   [dumpstr.core.db :as db]
   [dumpstr.core.admin :as admin]
   [dumpstr.core.user :as user]))

(defroutes admin-routes
  (GET "/show-user/:user" [user] (admin/show-user user))
  (GET "/all-users" [] (admin/all-users)))

(defroutes user-routes
  (GET "/hi" [] (str "well hi, " (:username (friend/current-authentication))))
  (GET "/bye" [] (str "ok bye, ")))

(defn- build-json-response [{:keys [success] :as  params}]
  (let [response (resp/response (json/generate-string params))]
    (if success
      response
      (resp/status response 400))))

(defroutes app-routes
  ;; User auth routes
  (context "/u" request
    (friend/wrap-authorize user-routes #{:user}))

  ;; Admin auth routes
  (context "/a" request
    (friend/wrap-authorize admin-routes  #{:admin}))

  ;; no auth required
  (GET "/" [] (h/html [:h1 "Go get some l¡ttr!!1!"]))
  (GET "/dump" request [:h2 (str request)])
  (POST "/create-user" {:keys [params]}
        (build-json-response (user/create-user (:user component) params)))
  (ANY "/info" [] (str "API version: " util/project-version))
  (friend/logout  (ANY "/logout" request (resp/redirect "/")))

  ;; (GET ["/u/:cmd", :cmd #"[0-9]+"]  [cmd]  (str "Numeric cmd " cmd))
  ;; (GET "/u/:cmd" [cmd]  (str "Other cmd " cmd))


  (route/not-found "Not Found"))

;; (def app
;;   (->
;;       (sess/wrap-session app-routes)
;;     (wrap-defaults site-defaults)))

(def secured-app
  (-> app-routes
    (friend/authenticate
     {:allow-anon? true
      :unauthenticated-handler #(workflows/http-basic-deny "Littr" %)
      :credential-fn (partial creds/bcrypt-credential-fn db/get-user)
      :workflows [(workflows/http-basic
                   :credential-fn #(creds/bcrypt-credential-fn
                                    (partial db/get-user :username)  %)
                   :realm "Littr")]})))

(defn make-approutes
  [component]
  (routes
   (GET "/" [] (h/html [:h1 "Go get some l¡ttr!!1!"]))
   (GET "/dump" request [:h2 (str request)])
   (POST "/create-user" {:keys [params]}
         (build-json-response (user/create-user params)))
   (ANY "/info" [] (str "API version: " util/project-version))))

;; (def site
;;   wrap-defaults handler site-defaults)

(def app (site secured-app))

(defrecord HttpServer [port server user]
  component/Lifecycle

  (start [component]
    (println "Starting HTTP Server")
    (let [server (jetty/run-jetty app {:port port :join? false})]
      (assoc component :server server)))

  (stop [component]
    (println "Stopping HTTPServer")
    (.stop server)
    component))

(defn new-http-server
  [port]
  (map->HttpServer {:port port}))
  (map->HTTPServer {:port port}))

;; (define-clojure-indent
;;   (defroutes 'defun)
;;   (GET 2)
;;   (POST 2)
;;   (PUT 2)
;;   (DELETE 2)
;;   (HEAD 2)
;;   (ANY 2)
;;   (context 2))
