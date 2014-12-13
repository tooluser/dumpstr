(ns dumpstr.core.user
  (:require
   [dumpstr.core.db :as db]))

(defn create-user [params]
  ;; TODO: strip out invalid keys
  ;; TODO: crypt passwd
  ;; TODO: set roles
  (let [un (:username params)]
    (if (db/get-user un)
      (str "JSON: User already exists")
      (do
        (db/create-user params)
        (str "JSON: I will create user " un " (" (:email params) ")")))))
    
(defn show-user [user]
  (str (db/get-user user)))
