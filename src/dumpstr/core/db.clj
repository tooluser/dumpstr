(ns dumpstr.core.db
  (:require
   [taoensso.faraday :as far]
   [clojure.string :as string])
  (:import  [com.amazonaws.auth BasicAWSCredentials]
            [com.amazonaws.services.dynamodbv2.model ConditionalCheckFailedException]))

;; Local access only for now
(defn client-opts []
  {:access-key "ACCESS_KEY"
   :secret-key "SECRET_KEY"
   :endpoint "http://localhost:8000"})

(defmacro dbg[x] `(let [x# ~x] (println "dbg:" '~x "=" x#) x#))

(defn create-tables []
  (far/create-table
   (client-opts)
   :users
   [:id :s]
   {:throughput {:read 1 :write 1}
    :block? true})
  (far/create-table
   (client-opts)
   :emails
   [:email :s]
   {:throughput {:read 1 :write 1}
    :block? true})
  (far/create-table
   (client-opts)
   :usernames
   [:username :s]
   {:throughput {:read 1 :write 1}
    }))

(defn delete-tables []
  (far/delete-table (client-opts) :users)
  (far/delete-table (client-opts) :emails)
  (far/delete-table (client-opts) :usernames))

(defn num-users []
  (:item-count (far/describe-table (client-opts) :users)))

(def param-table {:email :emails, :username :usernames})

(defn check-param [tag request]
  (try
    (when (request tag)
      (far/put-item (client-opts) (param-table tag)
                    {tag (request tag) :id (request :id)}
                    {:expected {tag false}}))
    true
    (catch ConditionalCheckFailedException e
      nil)))

(defn create-user-record
  [{:keys [email username id roles] :as request}]
  (try
    (far/put-item (client-opts) :users
                  (assoc request :roles (far/freeze roles))
                  {:expected {:id false}})
    (assoc request :success true)
    (catch ConditionalCheckFailedException e
         (when username
           (far/delete-item (client-opts)
                            :usernames {:username username}))
         (when email
           (far/delete-item (client-opts)
                            :emails {:email email}))
         {:success false, :error "Id already exists"})))

(defn create-user-with-unique-fields [[tag & rest-of-tags] request]
  (if (nil? tag)
    (create-user-record request)
    (if (check-param tag request)
      (recur rest-of-tags request)
      {:success false
       :error (string/capitalize
               (str (name tag) " already exists"))})))

(defn create-user
  [request]
  (create-user-with-unique-fields [:email :username] request))

(defn modify-user-record
  [{:keys [email username id] :as request}
   old-user]
  (let [delete (fn [t v] (when v
                           (far/delete-item
                            (client-opts)
                            (param-table t)
                            {t v})))]
    (far/update-item (client-opts) :users {:id id}
                     (reduce-kv #(assoc %1 %2 [:put %3])
                                {}
                                (dissoc request :id)))
    (delete :email (:email old-user))
    (delete :username (:username old-user))
    (assoc request :success true)))

(defn modify-user-with-unique-fields
  [[tag & rest-of-tags] request old-user]
  (if (nil? tag)
    (modify-user-record request old-user)
    (if (check-param tag request)
      (recur rest-of-tags request old-user)
      {:success false
       :error (string/capitalize
               (str (name tag) " already exists"))})))

(defn- get-user-id [key value consistent?]
  (:id (far/get-item (client-opts) (param-table key) {key value}
                     consistent?)))

(defn get-user [key value & [{:keys [consistent?]}]]
  (let [consistent? {:consistent? consistent?}]
    (cond
      (nil? value) nil
      (= key :id)
      (far/get-item (client-opts) :users {:id value} consistent?)
      :else
      (get-user :id (get-user-id key value consistent?)))))

(defn delete-user-id [id]
  (let [{:keys [email username]} (get-user :id id)]
    (far/delete-item (client-opts) :emails {:email email})
    (far/delete-item (client-opts) :usernames {:username username})
    (far/delete-item (client-opts) :users {:id id})))


(defn modify-user [request]
  (let [old-user (get-user :id (:id request))]
    (if (nil? old-user)
      {:success false :error "No such user"}
      (modify-user-with-unique-fields [:email :username]
                                      request old-user))))
