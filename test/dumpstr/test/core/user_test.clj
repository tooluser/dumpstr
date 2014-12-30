(ns dumpstr.test.core.user-test
  (:use midje.sweet)
  [:require [dumpstr.core.user :refer :all]
   [midje.config :as config]
   [me.raynes.conch.low-level :as sh]
   [dumpstr.core.db :as db]])

(def test-clientopts
  {:access-key "ACCESS_KEY"
   :secret-key "SECRET_KEY"
   :endpoint "http://localhost:8080"})

(background (db/client-opts) => test-clientopts)

(defn- start-local-ddb []
  (let [proc (sh/proc "java"
                      "-Djava.library.path=/Users/dangit/.homebrew/Cellar/dynamodb-local/2014-10-07/libexec/DynamodbLocal_lib"
                      "-jar"
                      "/Users/dangit/.homebrew/Cellar/dynamodb-local/2014-10-07/libexec/DynamoDBLocal.jar"
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

 (facts "About create-user"
        (against-background
         [(around
           :facts
           (let [ddb-proc (start-local-ddb)]
             (db/create-tables)
             ?form
             (stop-local-ddb ddb-proc)))]
         (fact "Can create a user"
             (let [{:keys [success id username email]}
                  (create-user {:id "1",
                                :username "joe",
                                :email "joe@bob.com",
                                :password "secret"})]
              success  => truthy
              id => "1"
              username => "joe"
              email => "joe@bob.com"))
        (fact "No password is returned"
            (let [response (create-user {:username "sally"
                                         :password "secret"})]
              (:success response) => truthy
              (:passowrd response) => falsey))
        (fact "First user created as an admin"
            (get-user-roles "joe") => (contains :admin :user))
        (fact "Blessed users are created as admin"
            (get-user-roles "first") => irrelevant
            (get-user-roles "dan") => (contains :admin :user)
            (get-user-roles "tooluser") => (contains :admin :user)
            (get-user-roles "matt") => (contains :admin :user))
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
            (:success (create-user {:username "dummy"})) => falsey)
        (fact "User created with dupe id fails"
            (:success (do
                        (create-user {:id "1", :username "samers"
                                      :password "s"})
                        (create-user {:id "1", :username "differs"
                                      :password "s"}))) =>
                                      falsey)
        (fact "User created with dupe username fails"
            (:success (do
                        (create-user {:id "1", :username "samers"
                                      :password "s"})
                        (create-user {:id "2", :username "samers"
                                      :password "s"}))) =>
                                      falsey)
        (fact "User created with dupe email fails"
            (:success (do
                        (create-user {:id "1", :email "samers@test.com"
                                      :password "s"})
                        (create-user {:id "2", :username "samers@test.com"
                                      :password "s"}))) =>
                                      falsey)
       ;; How to test this here?
       ;;        (fact "Race condition for dupe username resolves correctly" true => falsey))
        ))

(facts "About get-user"
       (against-background
        [(around
          :facts
          (let [ddb-proc (start-local-ddb)]
            (db/create-tables)
            ?form
            (stop-local-ddb ddb-proc)))]
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
                 response (get-user :username "t1@t.com")]
             (:success response) => truthy
             (:id response) => (:id created)))
       (fact "Cannot lookup user by random tag"
           (let [created (create-user {:username "t2"
                                       :password "secret"
                                       :photo-url "http://beautiful.me"})
                 response (get-user :photo-url "http://beautiful.me")]
             (:success response) => falsey))))

