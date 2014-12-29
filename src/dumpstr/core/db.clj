(ns dumpstr.core.db
  (:require
   [taoensso.faraday :as far])
  (:import  [com.amazonaws.auth BasicAWSCredentials]))

;; Local access only for now
(def client-opts
  {:access-key "ACCESS_KEY"
   :secret-key "SECRET_KEY"
   :endpoint "http://localhost:8000"})

(defmacro dbg[x] `(let [x# ~x] (println "dbg:" '~x "=" x#) x#))

(defn create-tables []
  (far/create-table
   client-opts
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


(defn num-users []
  (:item-count (far/describe-table client-opts :users)))

(defn create-user
  [{:keys [username email password photo-url roles id] :as request}]
  (far/put-item client-opts :users request))

(defn get-user [key value]
  (case key
    :id
    [(far/get-item client-opts :users {:id value})]
    :email
    (seq (far/scan client-opts :users
                   {:attr-conds {:email [:eq value]}}))
    :username
    (seq (far/scan client-opts :users
                   {:attr-conds {:username [:eq value]}}))))
    
