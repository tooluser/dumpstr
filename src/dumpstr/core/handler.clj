(ns dumpstr.core.handler
  (:require
   [compojure.core :refer :all]
   [compojure.route :as route]
   [compojure.handler :refer (site)]
   [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
   [ring.middleware.session :as sess]
   [ring.util.response :as resp]
   [hiccup.core :as h]
   [hiccup.element :as e]
   [cemerick.friend :as friend]
   (cemerick.friend [workflows :as workflows]
                    [credentials :as creds])
   [dumpstr.core.db :as db]))

;; Dummy user DB
(def users {"dan" {:username "dan"
                   :password (creds/hash-bcrypt "idan")
                   :roles #{::admin}}
            "bob" {:username "bob",
                   :password (creds/hash-bcrypt "gobob")
                   :roles #{::user}}})

(defn get-user [un]
  (db/get-user un))

(derive ::admin ::user)

(defroutes admin-routes
  (GET "/show-user/:user" [user] (str (get-user user)))
  (GET "/bye" [] (str "ok bye, admin")))

(defroutes user-routes
  (GET "/hi" [] (str "well hi, " (:username (friend/current-authentication))))
  (GET "/bye" [] (str "ok bye, ")))

(defroutes app-routes
  ;; User auth routes
  (context "/u" request
    (friend/wrap-authorize user-routes #{::user}))

  ;; Admin auth routes
  (context "/a" request
    (friend/wrap-authorize admin-routes  #{::admin}))

  ;; no auth required
  (GET "/" [] (h/html [:h1 "Go get some l¡ttr!!1!"]))
;;  (GET "/create-user" [] (str "I will create user" ))
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
      :credential-fn (partial creds/bcrypt-credential-fn get-user)
      :workflows [(workflows/http-basic
                   :credential-fn #(creds/bcrypt-credential-fn users %)
                   :realm "Littr")]})))

(def app (site secured-app))

;; (define-clojure-indent
;;   (defroutes 'defun)
;;   (GET 2)
;;   (POST 2)
;;   (PUT 2)
;;   (DELETE 2)
;;   (HEAD 2)
;;   (ANY 2)
;;   (context 2))
