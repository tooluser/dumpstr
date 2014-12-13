(ns dumpstr.core.user
  (:require
   (cemerick.friend [workflows :as workflows]
                    [credentials :as creds])
   [dumpstr.core.db :as db]))

(derive ::admin ::user)


(defn create-user [params]
  ;; TODO: strip out invalid keys
  ;; TODO: check for required keys
  ;; TODO: sanitize input?
  (let [un (:username params)]
    (if (db/get-user un)
      (str "JSON: User already exists")
      (let [roles (if (zero? (db/num-users)) #{::admin} #{::user})
            password (creds/hash-bcrypt (:password params))]
        (db/create-user (assoc params
                               :roles roles
                               :password password))
        (str "JSON: I will create user " un " (" (:email params) ")")))))
    
(defn show-user [user]
  (str (db/get-user user)))
