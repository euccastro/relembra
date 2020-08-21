(ns relembra.db-model.user
  (:require [clj-uuid :as uuid]
            [relembra.crux :refer [q1 sync-tx]]))


(defn get-user-id [crux-node name]
  (q1 crux-node
      {:find ['u]
       :where [['u :user/name name]]}))


(defn get-hashed-password [crux-node user-id]
  (q1 crux-node
      {:find ['p]
       :where [[user-id :user/hashed-password 'p]]}))


(defn add-user [crux-node name hashed-password]
  (when-let [uid (get-user-id crux-node name)]
    (throw (ex-info "user already exists" {:name name :uid uid})))
  (let [uid (uuid/v1)]
    (sync-tx crux-node
             [[:crux.tx/match uid nil]
              [:crux.tx/put {:crux.db/id uid
                             :user/name name
                             :user/hashed-password hashed-password}]])))


(comment

  (do
    (require '[integrant.repl.state :refer [system]])
    (def crux-node (:relembra.crux/node system)))

  (add-user crux-node "es" "nathoesunathoe-pass")
  (def uid (get-user-id crux-node "es"))
  (get-hashed-password crux-node uid)
  )
