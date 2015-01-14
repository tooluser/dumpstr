(ns dumpstr.core.user
  (:require
   (cemerick.friend [workflows :as workflows]
                    [credentials :as creds])
   [clj-time.core :as t]
   [clj-time.coerce :as tc]
   [com.stuartsierra.component :as component]
   [dumpstr.core.db :as db]))

;;(derive ::admin ::user)

(def valid-user-keys    [:id :username :email :password :photo-url])
(def required-user-keys [:password])
(def returned-user-keys [:id :username :email :photo-url :roles])
(def queriable-tags #{:id :username :email})

(defrecord User [config db]
  component/Lifecycle

  (start [component]
    (prn "Staring User")
    (assoc component :db db))

  (stop [component]
    (prn "Stopping User")
    (assoc component :db nil)))

(defn new-user [config]
  (map->User {:config config}))

(defmacro dbg[x] `(let [x# ~x] (println "dbg:" '~x "=" x#) x#))

(defn- generate-uuid [] (str (java.util.UUID/randomUUID)))

(defn- success
  ([] (success {}))
  ([m] (assoc m :success true)))

(defn- maybe-success
  [{:keys [suc] :as m} ]
  (if-not (= suc false)
    (success m)
    m))

(defn- failure
  ([err] (failure {} err))
  ([m err] (into m {:success false :error err})))

(defn- should-be-admin? [user username]
  (or (zero? (db/num-users (:db user)))
      (#{"tooluser" "matt" "dan"} username)))

(defn create-user [user {:keys [username email id] :as params}]
  (let [id (or id (generate-uuid))
        params (select-keys (assoc params :id id) valid-user-keys)]
    (cond
      (not (reduce #(and %1 (contains? params %2))
                   true required-user-keys))
      (failure params "Incomplete request")
      :else
      (let [roles (if (should-be-admin? user username)
                    #{:admin :user} #{:user})
            password (creds/hash-bcrypt (:password params))]
        (db/create-user (:db user)
         (assoc params
                :roles roles
                :password password
                :timestamp (tc/to-long (t/now))))))))

(defn modify-user
  [user {:keys [id] :as  request}]
  (let [request (select-keys request valid-user-keys)]
    (if (nil? id)
      {:success false :error "Bad request"}
      (db/modify-user (:db user) request))))

(defn get-user [user tag value]
  (if (contains? queriable-tags tag)
    (if-let [user (db/get-user (:db user) tag value)]
      (success (select-keys user returned-user-keys))
      (failure "No such user"))
    (failure "Bad query")))

(defn delete-user [user id]
  (maybe-success (db/delete-user-id (:db user) id)))

(defn all-users []
  (map #(dissoc % :password) (db/all-users)))
