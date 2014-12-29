(ns dumpstr.core.user
  (:require
   (cemerick.friend [workflows :as workflows]
                    [credentials :as creds])
   [dumpstr.core.db :as db]))

;;(derive ::admin ::user)

(def valid-user-keys    [:id :username :email :password :photo-url])
(def required-user-keys [:password])
(def returned-user-keys [:id :username :email :photo-url :roles])
(def queriable-tags #{:id :username :email})


(defmacro dbg[x] `(let [x# ~x] (println "dbg:" '~x "=" x#) x#))

(defn- generate-uuid [] (str (java.util.UUID/randomUUID)))

(defn- should-be-admin? [user]
  (or (zero? (db/num-users)) (#{"tooluser" "matt" "dan"} user)))


(defn create-user [{:keys [username email id] :as params}]
  (let [id (or id (generate-uuid))
        params (select-keys (assoc params :id id) valid-user-keys)]
    (cond
      (not (reduce #(and %1 (contains? params %2))
                   true required-user-keys))
      (assoc params :success false :error "Incomplete request")
      :else
      (let [roles
            (if (should-be-admin? username) #{:admin :user} #{:user})
            password (creds/hash-bcrypt (:password params))]
        (db/create-user (assoc params :roles roles :password password))
        (assoc (select-keys params returned-user-keys)
               :roles roles
               :success true)))))

;; (defn create-user [{:keys [username email id] :as params}]
;;   (let [id (or id (generate-uuid))
;;         params (assoc params :id id)
;;         resp {:username username :email email :id id}]
;;     (cond
;;       (db/get-user :username username)
;;       (assoc resp :success false :error "Username already exists")
;;       (db/get-user :email email)
;;       (assoc resp :success false :error "Email already exists")
;;       (and id (db/get-user :id id))
;;       (assoc resp :success false :error "ID already exists")
;;       (not (reduce #(and %1 (contains? params %2))
;;                    true required-user-keys))
;;       (assoc resp :success false :error "Incomplete request")
;;       :else (let [roles (if (should-be-admin? username)
;;                           #{:admin :user} #{:user})
;;                   password (creds/hash-bcrypt (:password params))
;;                   user (select-keys params valid-user-keys)]
;;               (db/create-user (assoc user :roles roles :password password))
;;               (assoc resp :success true)))))


(defn get-user [field user]
  (if (contains? queriable-tags field)
    (assoc (first (db/get-user field user)) :success true)
    {:success false, :error "Bad query"}))
