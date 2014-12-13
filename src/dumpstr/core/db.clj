(ns dumpstr.core.db
  (:require
   [taoensso.faraday :as far])
  (:import  [com.amazonaws.auth BasicAWSCredentials]))

;; Local access only for now
(def client-opts
  (let [provider (BasicAWSCredentials. "dan" "dan")]
    {:access-key "ACCESS_KEY"
     :secret-key "SECRET_KEY"
     :endpoint "http://localhost:8000"}))


(defn create-tables []
  (far/create-table
   client-opts
   :users
   [:username :s]
   {:throughput {:read 1 :write 1}
    :block? true}
   ))

(defn create-user [user]
  (far/put-item client-opts :users user))

(defn get-user [username]
  (far/get-item client-opts :users {:username username}))
    
