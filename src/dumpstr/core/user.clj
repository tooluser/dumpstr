(ns dumpstr.core.user
  (:require
   (cemerick.friend [workflows :as workflows]
                    [credentials :as creds])
   [dumpstr.core.db :as db]))

;;(derive ::admin ::user)

(def valid-user-keys    [:id :username :email :password :photo-url])
(def required-user-keys [:id :username :email :password])


(defmacro dbg[x] `(let [x# ~x] (println "dbg:" '~x "=" x#) x#))

(defn- should-be-admin? [user]
  (or (zero? (db/num-users)) (#{"tooluser" "matt" "dan"} user)))

(defn create-user [params]
  (let [{:keys [username email id]} params
        resp {:username username :email email :id id}]
    (cond
      (db/get-user :username username)
      (assoc resp :success false :error "Username already exists")
      (db/get-user :email email)
      (assoc resp :success false :error "Email already exists")
;      (db/get-user :id id)
;      (assoc resp :success false :error "ID already exists")
      (not (reduce #(and %1 (contains? params %2))
                   true required-user-keys))
      (assoc resp :success false :error "Incomplete request")
      :else (let [roles (if (should-be-admin? username)
                          #{:admin :user} #{:user})
                  password (creds/hash-bcrypt (:password params))
                  user (select-keys params valid-user-keys)]
              (db/create-user (assoc user :roles roles :password password))
              (assoc resp :success true)))))
    
(defn get-user [user]
  (db/get-user :username user))
