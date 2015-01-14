(ns dumpstr.core.db
  (:require
   [taoensso.faraday :as far]
   [environ.core :as env]
   [com.stuartsierra.component :as component]
   [clojure.string :as string])
  (:import  [com.amazonaws.auth BasicAWSCredentials]
            [com.amazonaws.services.dynamodbv2.model ConditionalCheckFailedException]))

(defrecord Database [client-opts]
  component/Lifecycle

  (start [component]
    (prn "Starting Database")
    (assoc component :client-opts client-opts))

  (stop [component]
    (prn "Stopping Database")
    (assoc component :client-opts nil)))

(defn new-database [client-opts]
  (map->Database {:client-opts client-opts}))


;; Local access only for now
(defn client-opts []
  (let [access-key (env/env :ddb-access-key)
        secret-key (env/env :ddb-secret-key)]
    (if (and access-key secret-key)
      {:access-key access-key,
       :secret-key secret-key}
      {:access-key "ACCESS-KEY",
       :secret-key "SECRET-KEY",
       :endpoint "http://localhost:8000"})))

(defmacro dbg[x] `(let [x# ~x] (println "dbg:" '~x "=" x#) x#))

(defn create-tables [db]
  (far/create-table
   (:client-opts db)
   :litter-users
   [:id :s]
   {:throughput {:read 1 :write 1}
    :block? true})
  (far/create-table
   (:client-opts db)
   :littr-emails
   [:email :s]
   {:throughput {:read 1 :write 1}
    :block? true})
  (far/create-table
   (:client-opts db)
   :littr-usernames
   [:username :s]
   {:throughput {:read 1 :write 1}
    }))

(defn delete-tables [db]
  (far/delete-table (:client-opts db) :litter-users)
  (far/delete-table (:client-opts db) :littr-emails)
  (far/delete-table (:client-opts db) :littr-usernames))

(defn num-users [db]
  (:item-count (far/describe-table (:client-opts db) :litter-users)))

(def param-table {:email :littr-emails, :username :littr-usernames})

(defn- check-param [db tag request]
  (try
    (when (request tag)
      (far/put-item (:client-opts db) (param-table tag)
                    {tag (request tag) :id (request :id)}
                    {:expected {tag false}}))
    true
    (catch ConditionalCheckFailedException e
      nil)))

(defn- create-user-record
  [db {:keys [email username id roles] :as request}]
  (try
    (far/put-item (:client-opts db) :litter-users
                  (assoc request :roles (far/freeze roles))
                  {:expected {:id false}})
    (assoc request :success true)
    (catch ConditionalCheckFailedException e
         (when username
           (far/delete-item (:client-opts db)
                            :littr-usernames {:username username}))
         (when email
           (far/delete-item (:client-opts db)
                            :littr-emails {:email email}))
         {:success false, :error "Id already exists"})))

(defn do-with-unique-fields [db action request [tag & rest-of-tags]]
  (if (nil? tag)
    (action request)
    (if (check-param db tag request)
      (recur db action request rest-of-tags)
      {:success false
       :error (str (string/capitalize (name tag)) " already exists")})))

(defn create-user
  [db request]
  (do-with-unique-fields db (partial create-user-record db) request [:email :username]))

(defn modify-user-record
  [db old-user
   {:keys [email username id] :as request}]
  (let [delete (fn [t v] (when v
                           (far/delete-item
                            (:client-opts db)
                            (param-table t)
                            {t v})))]
    (far/update-item (:client-opts db) :litter-users {:id id}
                     (reduce-kv #(assoc %1 %2 [:put %3])
                                {}
                                (dissoc request :id)))
    (delete :email (:email old-user))
    (delete :username (:username old-user))
    (assoc request :success true)))

(defn- get-user-id [db key value consistent?]
  (:id (far/get-item (:client-opts db) (param-table key) {key value}
                     consistent?)))

(defn get-user [db key value & [{:keys [consistent?]}]]
  (let [consistent? {:consistent? consistent?}]
    (cond
      (nil? value) nil
      (= key :id)
      (far/get-item (:client-opts db) :litter-users {:id value} consistent?)
      :else
      (get-user :id (get-user-id key value consistent?)))))

(defn delete-user-id [db id]
  (let [{:keys [email username]} (get-user :id id)]
    (when email
      (far/delete-item (:client-opts db) :littr-emails {:email email}))
    (when username
      (far/delete-item (:client-opts db) :littr-usernames {:username username}))
    (far/delete-item (:client-opts db) :litter-users {:id id})))


(defn modify-user [db request]
  (let [old-user (get-user :id (:id request))]
    (if (nil? old-user)
      {:success false :error "No such user"}
      (do-with-unique-fields db
                             (partial modify-user-record db old-user)
                             request [:email :username]))))

(defn all-users [db]
  (far/scan (:client-opts db) :litter-users))
