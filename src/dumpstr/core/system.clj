(ns dumpstr.core.system
  (:require
   [dumpstr.core.db :as db]
   [dumpstr.core.user :as user]
   [dumpstr.core.handler :as handler]
   [com.stuartsierra.component :as component]))

(defn littr-system [config]
  (component/system-map
   :db (db/new-database (:client-opts config))
   :user (component/using
          (user/new-user config)
          {:db :db})
   :https (component/using
           (handler/new-http-server config)
           {:db :db
            :user :user})))


