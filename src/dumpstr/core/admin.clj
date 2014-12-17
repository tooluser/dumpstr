(ns dumpstr.core.admin
  (:require
   [hiccup.core :as h]
   [hiccup.element :as e]
   [dumpstr.core.db :as db]
   [dumpstr.core.user :as user]))


(defn show-user [username]
  (let [user (db/get-user :username username)]
    (h/html [:html [:table (for [k (filter #(not= :password %) (keys user))]
                             [:tr [:td k [:td (user k)]]])]])))
