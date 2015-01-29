(ns dumpstr.core.admin
  (:require
   [hiccup.core :as h]
   [hiccup.element :as e]
   [taoensso.timber :as timbre]
   [dumpstr.core.db :as db]
   [dumpstr.core.user :as user]))

(timbre/refer-timbre)

(defmacro dbg[x] `(let [x# ~x] (println "dbg:" '~x "=" x#) x#))


(defn show-user [username]
  (debug "show-user" username)
  (let [user (db/get-user :username username)]
    (h/html [:html [:table (for [k (filter #(not= :password %) (keys user))]
                             [:tr [:td k [:td (user k)]]])]])))

(defn get-vals [m [k & ks]]
  (dbg m)
  (if (nil? k)
    nil
    (cons (m k) (get-vals m ks))))

(def shown-user-fields [:id :username :email :roles :photo-url])

(defn all-users []
  (debug "all-users")
  (let [mkcells (fn [v] (map #(vector :td %) (get-vals v shown-user-fields)))
        mkrows  (fn [v] (vector :tr (mkcells v)))]
    (h/html
     [:table {:border "1"}
      [:tr (map #(vector :th %) shown-user-fields)]
        (map mkrows (user/all-users))])))
