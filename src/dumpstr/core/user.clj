(ns dumpstr.core.user
  (:require
   (cemerick.friend [workflows :as workflows]
                    [credentials :as creds])
   [dumpstr.core.db :as db]))

;;(derive ::admin ::user)

(defn valid-user-keys [:username :photo-url :email])


(defn create-user [params]
  ;; TODO: check for required keys
  ;; TODO: sanitize input?
  (let [un (:username params)]
    (if (db/get-user un)
      (str "JSON: User already exists")
      (let [roles (if (zero? (db/num-users)) #{:admin} #{:user})
            password (creds/hash-bcrypt (:password params))
            user (select-keys params valid-user-keys)]
        (db/create-user (assoc user :roles roles :password password))
        (str "JSON: I will create user " un " (" (:email params) ")")))))
    
(defn show-user [user]
  (str (db/get-user user)))
