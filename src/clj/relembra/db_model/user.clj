(ns relembra.db-model.user
  (:require [clj-uuid :as uuid]
            [relembra.crux :refer [q1 sync-tx]]
            [relembra.schema :refer [user?]]))


(defn get-user-id [crux-node name]
  (q1 crux-node
      {:find ['u]
       :where [['u :relembra.user/name name]]}))


(defn get-hashed-password [crux-node user-id]
  (q1 crux-node
      {:find ['p]
       :where [[user-id :relembra.user/hashed-password 'p]]}))


(defn add-user [crux-node name hashed-password]
  (when-let [uid (get-user-id crux-node name)]
    (throw (ex-info "user already exists" {:name name :uid uid})))
  (let [uid (uuid/v1)
        user {:crux.db/id uid
              :relembra.user/name name
              :relembra.user/hashed-password hashed-password}]
    (if (user? user)
      (sync-tx crux-node
               [[:crux.tx/match uid nil]
                [:crux.tx/put user]])
      (throw (ex-info "invalid user?" {:user user})))))


(comment

  (do
    (require '[integrant.repl.state :refer [system]])
    (def crux-node (:relembra.crux/node system)))

  (def uid (get-user-id crux-node "es"))
  (get-hashed-password crux-node uid)

  (add-user crux-node "lhou" "senhahashada")

  (get-hashed-password crux-node (get-user-id crux-node "lhou"))
  )
