(ns dumpstr.core.db
  (:require
   [taoensso.faraday :as far])
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
    :block? true
    :gsindexes [{:name "username"
                 :hash-keydef [:username :s]
                 :projection :all
                 :throughput {:read 1 :write 1}}
                {:name "email"
                 :hash-keydef [:email :s]
                 :projection :all
                 :throughput {:read 1 :write 1}}]}))

(defn delete-tables []
  (far/delete-table (client-opts) :users))

(defn num-users []
  (:item-count (far/describe-table (client-opts) :users)))

(defn create-user
  [{:keys [roles] :as request}]
  (try
    (far/put-item (client-opts) :users
                  (assoc request :roles (far/freeze roles))
                  {:expected {:id false}})
    ;; {:return :all-new} doesn't seem to be working
    (assoc request :success true)
    (catch ConditionalCheckFailedException e
      {:success false :error "Id already exists"})))

(defn get-user [key value & [{:keys [consistent?]}]]
  (let [consistent? {:consistent? consistent?}]
    (case key
      :id
      [(far/get-item (client-opts) :users {:id value}) consistent?]
      :email
      (seq (far/scan (client-opts) :users
                     {:attr-conds {:email [:eq value]}}
                     consistent?))
      :username
      (seq (far/scan (client-opts) :users
                     {:attr-conds {:username [:eq value]}}
                      consistent?)))))

(defn delete-user-id [id]
  (far/delete-item (client-opts) :users {:id id}))
