(ns dumpstr.test.core.user-test
  (:use midje.sweet)
  [:require
   [clojure.java.io :as jio]
   [dumpstr.core.user :refer :all]
   [me.raynes.conch.low-level :as sh]
   [dumpstr.core.db :as db]])

(def test-clientopts
  {:access-key "ACCESS_KEY"
   :secret-key "SECRET_KEY"
   :endpoint "http://localhost:8080"})

(background (db/client-opts) => test-clientopts)

(defn- start-local-ddb []
  (let [ddb (jio/resource "ddb")
        proc (sh/proc "java"
                      (str "-Djava.library.path=" (.getPath ddb) "/DynamodbLocal_lib")
                      "-jar"
                      (str (.getPath ddb) "/DynamoDBLocal.jar")
                      "-inMemory"
                      "-port"
                      "8080")]
    (Thread/sleep 1000)
    proc))

(defn- stop-local-ddb [ddb-proc]
  (.destroy (:process ddb-proc)))

(defn- get-user-roles [un]
  (:roles  (create-user {:username un
                         :email (str un "@test.com")
                         :password "secret"})))
(against-background
 [(around :contents
          (let [ddb-proc (start-local-ddb)]
            ?form
            (stop-local-ddb ddb-proc)))]
 (facts "About create-user"
        (against-background
         [(around
           :facts
           (do
             (db/create-tables)
             ?form
             (db/delete-tables)))]
         (fact "Can create a user"
               (let [{:keys [success id username email]}
                     (create-user {:id "1",
                                   :username "joe",
                                   :email "joe@bob.com",
                                   :password "secret"})]
                 success  => truthy
                 id       => "1"
                 username => "joe"
                 email    => "joe@bob.com"))
         (fact "No password is returned"
               (let [response (create-user {:username "sally"
                                            :password "secret"})]
                 (:success response)  => truthy
                 (:passowrd response) => falsey))
         (fact "First user created as an admin"
               (get-user-roles "joe") => (contains :admin :user))
         (fact "Blessed users are created as admin"
               (get-user-roles "first")    => irrelevant
               (get-user-roles "dan")      => (contains :admin :user)
               (get-user-roles "tooluser") => (contains :admin :user)
               (get-user-roles "matt")     => (contains :admin :user))
         (fact "Non-blessed users not admin"
               (get-user-roles "first") => irrelevant
               (get-user-roles "chump") => (just :user))
         (fact "Users created without ids get different ids"
               (:id (create-user {:username "waldorf" :email "w@test.com"}))
               =not=>
               (:id (create-user {:username "statler" :email "s@test.com"})))
         (fact "User created without email is ok"
               (:success (create-user {:username "jimbob"
                                       :password "secret"})) => truthy)
         (fact "User created without username is ok"
               (:success (create-user {:password "secret"})) => truthy)
         (fact "User created must have a password"
               (create-user {:username "dummy"}) => (contains
                                                     {:success false
                                                      :error "Incomplete request"}))
         (fact "User created with dupe id fails"
               (do
                 (create-user {:id "1", :username "samers"
                               :password "s"})
                 (create-user {:id "1", :username "differs"
                               :password "s"})) => (contains
                                                    {:success false
                                                     :error "Id already exists"}))
         (fact "User created with dupe username fails"
               (do
                 (create-user {:id "1", :username "samers"
                               :password "s"})
                 (create-user {:id "2", :username "samers"
                               :password "s"})) => (contains
                                                    {:success false
                                                     :error "Username already exists"}))
         (fact "User created with dupe email fails"
               (do
                 (create-user {:id "1", :email "samers@test.com"
                               :password "s"})
                 (create-user {:id "2", :email "samers@test.com"
                               :password "s"})) => (contains
                                                    {:success false
                                                     :error "Email already exists"}))
         (fact "User created with dupe username is removed from DB"
               (do
                 (create-user {:id "1", :username "samers"
                               :password "s"})
                 (create-user {:id "2", :username "samers"
                               :password "s"})
                 (:success (get-user :id "2"))) => falsey)
         (fact "User created with dupe email is removed from DB"
               (do
                 (create-user {:id "1", :email "samers"
                               :password "s"})
                 (create-user {:id "2", :email "samers"
                               :password "s"})
                 (:success (get-user :id "2"))) => falsey)
         (fact "Getting non-existent user fails"
               (get-user :id "202") => {:success false,
                                        :error "No such user"})
         ;; How to test this here?
         ;;        (fact "Race condition for dupe username resolves correctly" true => falsey))
         ))

 (facts "About get-user"
        (against-background
         [(around
           :facts
           (do
             (db/create-tables)
             ?form
             (db/delete-tables)))]
         (fact "Can lookup user by id"
               (let [created (create-user {:username "t1" :password "secret"})
                     response (get-user :id (:id created))]
                 (:success response) => truthy
                 (select-keys created [:id :username :email]) =>
                 (select-keys response [:id :username :email])))
         (fact "Can lookup user by username"
               (let [created (create-user {:username "t1" :password "secret"})
                     response (get-user :username "t1")]
                 (:success response) => truthy
                 (:id response) => (:id created)))
         (fact "Can lookup user by email"
               (let [created (create-user {:email "t1@t.com" :password "secret"})
                     response (get-user :email "t1@t.com")]
                 (:success response) => truthy
                 (:id response) => (:id created)))
         (fact "Cannot lookup user by random tag"
               (let [created (create-user {:username "t2"
                                           :password "secret"
                                           :photo-url "http://beautiful.me"})
                     response (get-user :photo-url "http://beautiful.me")]
                 (:success response) => falsey))
         (fact "Random tags are not returned"
               (:success (create-user {:username "tbutton"
                                       :password "secret"
                                       :rando "sauce"})) => truthy
                                       (:rando (get-user :username "tbutton")) => falsey))))

