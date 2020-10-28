(ns relembra.test-db
  (:require [clj-uuid :as uuid]
            [clojure.test :refer [deftest is use-fixtures]]
            [crux.api :as crux]
            [relembra.schema :as schema]
            [relembra.db-model.qa :as db-qa]
            [relembra.db-model.user :as db-user]
            [tick.alpha.api :as t]))


(defmacro throws-ex-info [expr]
  `(is (~'thrown? clojure.lang.ExceptionInfo ~expr)))


(defmacro does-not-throw [expr]
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
    (throws-ex-info (db-user/add-user *crux-node* user-name "some-password"))
    (does-not-throw (db-user/add-user *crux-node* other-user-name "some-password"))))


(defn- add-test-user []
  (db-user/add-user *crux-node* "test-user-name" "hashed-password"))


(deftest add-qa
  (let [user-id (add-test-user)
        q "what?"
        a "this"
        qa-id (db-qa/add-qa *crux-node* user-id q a)
        qa-entity (crux/entity (crux/db *crux-node*) qa-id)
        other-qa-id (uuid/v1)
        other-qa-q "some other question"]
    (is (not (db-qa/existing-question *crux-node* user-id other-qa-q)))
    (is (db-qa/existing-question *crux-node* user-id q))
    (is (schema/qa? qa-entity))
    (is (= {:crux.db/id qa-id
            :relembra.qa/owner user-id
            :relembra.qa/question q
            :relembra.qa/answer a}
           qa-entity))
    (is (db-qa/qa-exists? *crux-node* qa-id))
    (is (not (db-qa/qa-exists? *crux-node* other-qa-id)))
    (does-not-throw (db-qa/check-qa *crux-node* qa-id))
    (throws-ex-info (db-qa/check-qa *crux-node* other-qa-id))
    (is (= (dissoc (db-qa/initial-lembrando qa-id) :crux.db/id)
           (dissoc
            (crux/entity (crux/db *crux-node*)
                         (db-qa/qa->lembrando-id *crux-node* qa-id))
            :crux.db/id)))))


(defn- add-test-qa []
  (let [uid (add-test-user)
        q "what?"
        a "this"]
    {:user-id uid
     :q q
     :a a
     :qa-id (db-qa/add-qa *crux-node* uid q a)}))


(deftest update-qa
  (let [{:keys [qa-id q a user-id]} (add-test-qa)
        existing-q "so what, again?"
        old-ent {:crux.db/id qa-id
                 :relembra.qa/owner user-id
                 :relembra.qa/question q
                 :relembra.qa/answer a}
        new-ent (assoc old-ent
                       :relembra.qa/question "some other question"
                       :relembra.qa/answer "some other answer")]
    (db-qa/add-qa *crux-node* user-id existing-q a)
    (throws-ex-info
     (db-qa/update-qa *crux-node*
                    old-ent
                    (dissoc new-ent :relembra.qa/owner)))
    (throws-ex-info
     (db-qa/update-qa *crux-node*
                    old-ent
                    (assoc new-ent :relembra.qa/question existing-q)))
    (is (not (db-qa/update-qa *crux-node* new-ent new-ent)))
    (is (db-qa/update-qa *crux-node* old-ent new-ent))
    (is (not (db-qa/update-qa *crux-node* old-ent new-ent)))))


(deftest lembrando
  (let [{:keys [qa-id]} (add-test-qa)
        due-date (t/date)
        failing? true
        remembering-state [1.0 2.0]
        lid (db-qa/qa->lembrando-id *crux-node* qa-id)
        expected-lent {:crux.db/id lid
                       :relembra.lembrando/qa qa-id
                       :relembra.lembrando/due-date due-date
                       :relembra.lembrando/failing? failing?
                       :relembra.lembrando/remembering-state remembering-state}
        update-ret (db-qa/update-lembrando
                    *crux-node*
                    (crux/entity (crux/db *crux-node*) lid)
                    expected-lent)
        actual-lent (crux/entity (crux/db *crux-node*) lid)
        new-date (t/date "2222-02-02")
        new-failing? false
        new-remembering-state [3.0 4.0]
        new-expected-lent {:crux.db/id lid
                           :relembra.lembrando/qa qa-id
                           :relembra.lembrando/due-date new-date
                           :relembra.lembrando/failing? new-failing?
                           :relembra.lembrando/remembering-state new-remembering-state}
        update2-ret (db-qa/update-lembrando
                     *crux-node*
                     actual-lent
                     new-expected-lent)
        new-actual-lent (crux/entity (crux/db *crux-node*) lid)]
    (is update-ret)
    (is update2-ret)
    (throws-ex-info (db-qa/update-lembrando
                      *crux-node*
                      nil
                      {:relembra.lembrando/due-date due-date
                       :relembra.lembrando/failing? failing?
                       :relembra.lembrando/remembering-state remembering-state}))
    (is (schema/lembrando? actual-lent))
    (is (schema/lembrando? new-actual-lent))
    (is (= expected-lent actual-lent))
    (is (= new-expected-lent new-actual-lent))))


(deftest due-questions
  (let [{:keys [user-id qa-id]} (add-test-qa)
        qa2-id (db-qa/add-qa *crux-node* user-id "wat wat?" "yes yes")
        lent2 (crux/entity (crux/db *crux-node*) (db-qa/qa->lembrando-id *crux-node* qa2-id))
        qa3-id (db-qa/add-qa *crux-node* user-id "o rly" "you bet!")
        lent3 (crux/entity (crux/db *crux-node*) (db-qa/qa->lembrando-id *crux-node* qa3-id))]
    (db-qa/update-lembrando *crux-node*
                            lent2
                            (assoc lent2 :relembra.lembrando/due-date (t/yesterday)))
    (db-qa/update-lembrando *crux-node*
                            lent3
                            (assoc lent3 :relembra.lembrando/due-date (t/tomorrow)))
    ;; yesterday's and today's questions are included, but not tomorrow's
    (is (= #{(crux/entity (crux/db *crux-node*) qa-id)
             (crux/entity (crux/db *crux-node*) qa2-id)}
           (db-qa/due-questions *crux-node* user-id)))))

(comment
  (t/yesterday)

  (def lent {:relembra.lembrando/due-date
             (. java.time.LocalDate parse "2020-10-18"),
             :relembra.lembrando/failing? true,
             :relembra.lembrando/remembering-state [1.0 2.0],
             :crux.db/id #uuid "6e1532f0-1124-11eb-98cd-6ac9d2f914f9",
             :relembra.lembrando/qa #uuid "6e142180-1124-11eb-98cd-6ac9d2f914f9"})
  (schema/lembrando? lent)
  )
