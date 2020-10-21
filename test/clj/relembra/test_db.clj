(ns relembra.test-db
  (:require [clj-uuid :as uuid]
            [clojure.test :refer [deftest is use-fixtures]]
            [crux.api :as crux]
            [relembra.crux :refer [q1]]
            [relembra.schema :as schema]
            [relembra.db-model.lembrando :as db-lmb]
            [relembra.db-model.qa :as db-qa]
            [relembra.db-model.user :as db-user]
            [tick.alpha.api :as t]
            [re-frame.db :as db]))


(defmacro throws-ex-info? [expr]
  `(is (~'thrown? clojure.lang.ExceptionInfo ~expr)))


(defmacro does-not-throw? [expr]
  `(is (or ~expr true)))


(def ^:dynamic *crux-node* nil)


(defn db-fixture [f]
  (with-open [crux-node (crux/start-node {})]
    (binding [*crux-node* crux-node]
      (f))))

(use-fixtures :each db-fixture)


(deftest user
  (let [user-name "test-user-name"
        hashed-password "this-would-be-a-hashed-password"
        user-id (db-user/add-user *crux-node* user-name hashed-password)
        user-id' (db-user/get-user-id *crux-node* user-name)
        user-entity (crux/entity (crux/db *crux-node*) user-id)
        other-user-name "this-user-name-does-not-exist"
        other-user-id (uuid/v1)]
    (is (db-user/user-exists? *crux-node* user-id))
    (is (not (db-user/user-exists? *crux-node* other-user-id)))
    (is (= user-id (db-user/get-user-id *crux-node* user-name)))
    (is (nil? (db-user/get-user-id *crux-node* other-user-name)))
    (is (= hashed-password (db-user/get-hashed-password *crux-node* user-id)))
    (is (nil? (db-user/get-hashed-password *crux-node* other-user-id)))
    (is (= user-id user-id'))
    (is (schema/user? user-entity))
    (is (= {:crux.db/id user-id
            :relembra.user/name user-name
            :relembra.user/hashed-password hashed-password}
           user-entity))
    (throws-ex-info? (db-user/add-user *crux-node* user-name "some-password"))
    (does-not-throw? (db-user/add-user *crux-node* other-user-name "some-password"))))


(defn- add-test-user []
  (db-user/add-user *crux-node* "test-user-name" "hashed-password"))


(deftest qa
  (let [user-id (add-test-user)
        q "what?"
        a "this"
        qa-id (db-qa/add-qa *crux-node* user-id q a)
        qa-entity (crux/entity (crux/db *crux-node*) qa-id)
        other-qa-id (uuid/v1)
        other-qa-q "some other question"]
    (is (not (db-qa/existing-question? *crux-node* user-id other-qa-q)))
    (is (db-qa/existing-question? *crux-node* user-id q))
    (is (schema/qa? qa-entity))
    (is (= {:crux.db/id qa-id
            :relembra.qa/owner user-id
            :relembra.qa/question q
            :relembra.qa/answer a}
           qa-entity))
    (is (db-qa/qa-exists? *crux-node* qa-id))
    (is (not (db-qa/qa-exists? *crux-node* other-qa-id)))
    (does-not-throw? (db-qa/check-qa *crux-node* qa-id))
    (throws-ex-info? (db-qa/check-qa *crux-node* other-qa-id))))


(defn- add-test-qa []
  (let [uid (add-test-user)
        q "what?"
        a "this"]
    {:user-id uid
     :q q
     :a a
     :qa-id (db-qa/add-qa *crux-node* uid q a)}))


(deftest lembrando
  (let [{:keys [q a qa-id]} (add-test-qa)
        other-qa-id (uuid/v1)
        due-date (t/date)
        failing? true
        remembering-state [1.0 2.0]
        pre-lid (db-lmb/qa->lembrando-id *crux-node* qa-id)
        lid (db-lmb/update-lembrando
             *crux-node*
             qa-id
             nil
             {:relembra.lembrando/due-date due-date
              :relembra.lembrando/failing? failing?
              :relembra.lembrando/remembering-state remembering-state})
        lent (db-lmb/lembrando-entity *crux-node* lid)]
    (is (nil? pre-lid))
    (is (= lid (db-lmb/qa->lembrando-id *crux-node* qa-id)))
    (throws-ex-info? (db-lmb/update-lembrando
                      *crux-node*
                      other-qa-id
                      nil
                      {:relembra.lembrando/due-date due-date
                       :relembra.lembrando/failing? failing?
                       :relembra.lembrando/remembering-state remembering-state}))
    (is (not (schema/explain-lembrando lent)))
    (is (schema/lembrando? lent))))


(comment

  (require '[malli.core :as m])
  (def lent {:relembra.lembrando/due-date
             (. java.time.LocalDate parse "2020-10-18"),
             :relembra.lembrando/failing? true,
             :relembra.lembrando/remembering-state [1.0 2.0],
             :crux.db/id #uuid "6e1532f0-1124-11eb-98cd-6ac9d2f914f9",
             :relembra.lembrando/qa #uuid "6e142180-1124-11eb-98cd-6ac9d2f914f9"})
  (schema/lembrando? lent)
  )
