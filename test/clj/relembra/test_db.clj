(ns relembra.test-db
  (:require  [clojure.test :refer [deftest is use-fixtures]]
             [crux.api :as crux]
             [relembra.schema :as schema]
             [relembra.db-model.user :as db-user]
             [tick.alpha.api :as t]))


(def ^:dynamic *crux-node* nil)

(defn db-fixture [f]
  (with-open [crux-node (crux/start-node {})]
    (binding [*crux-node* crux-node]
      (f))))

(use-fixtures :each db-fixture)


(deftest user-round-trip
  (let [user-name "test-user-name"
        hashed-password "this-would-be-a-hashed-password"
        _ (db-user/add-user *crux-node* user-name hashed-password)
        user-id (db-user/get-user-id *crux-node* user-name)
        user-entity (crux/entity (crux/db *crux-node*) user-id)]
    (is (schema/user? user-entity))
    (is (= {:crux.db/id user-id
            :relembra.user/name user-name
            :relembra.user/hashed-password hashed-password}
           user-entity))))
