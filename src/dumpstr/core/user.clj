(ns dumpstr.core.user
  (:require
   (cemerick.friend [workflows :as workflows]
                    [credentials :as creds])
   [clj-time.core :as t]
   [clj-time.coerce :as tc]
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

(defn- create-new-user [{:keys [username email id] :as params}]
  (let [id (or id (generate-uuid))
        params (select-keys (assoc params :id id) valid-user-keys)]
    (cond
      (not (reduce #(and %1 (contains? params %2))
                   true required-user-keys))
      (assoc params :success false :error "Incomplete request")
      :else
      (let [roles (if (should-be-admin? username)
                    #{:admin :user} #{:user})
            password (creds/hash-bcrypt (:password params))]
        (db/create-user
         (assoc params
                :roles roles
                :password password
                :timestamp (tc/to-long (t/now))))))))

(defn- oldest-user [tag param]
  (first (sort-by :timestamp (db/get-user tag param))))

(defn create-user
  [{:keys [email username] :as params}]
  (let [user (create-new-user params)
        ts (:timestamp user)]
    (cond
      (not (:success user))
      user
      (and email (not= (:timestamp (oldest-user :email email)) ts))
      (do
        (db/delete-user-id (:id user))
        {:success false, :error "Email already exists"})
      (and username (not= (:timestamp (oldest-user :username username)) ts))
      (do
        (db/delete-user-id (:id user))
        {:success false, :error "Username already exists"})
      :else
      (assoc user :success true))))

(defn get-user [tag value]
  (if (contains? queriable-tags tag)
    (if-let [user (first (db/get-user tag value))]
      (assoc user :success true)
      {:success false, :error "No such user"})
    {:success false, :error "Bad query"}))
