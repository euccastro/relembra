(ns relembra.db-model.lembrando
  (:require [clj-uuid :as uuid]
            [relembra.crux :refer [q1 sync-tx]]
            [relembra.db-model.user :as db-user]
            [relembra.schema :refer [lembrando?]]
            [tick.alpha.api :as t]
            [crux.api :as crux]))


(defn user-exists? [crux-node user-id]
  (some? (db-user/get-hashed-password crux-node user-id)))


(defn add-lembrando [crux-node user-id question answer]
  (let [lid (uuid/v1)
        lembrando {:crux.db/id lid
                   :relembra.lembrando/user user-id
                   :relembra.lembrando/question question
                   :relembra.lembrando/answer answer
                   :relembra.lembrando/due-date (t/today)
                   :relembra.lembrando/failing? false}]
    (cond
      (not (lembrando? lembrando)) (throw (ex-info "invalid lembrando?" {:lembrando lembrando}))
      (not (user-exists? crux-node user-id)) (throw (ex-info "unknown user?" {:user-id user-id}))
      :else (sync-tx crux-node
                     [[:crux.tx/match lid nil]
                      [:crux.tx/put lembrando]]))))


(comment

  (do
    (require '[integrant.repl.state :refer [system]])
    (def crux-node (:relembra.crux/node system)))

  (def uid (db-user/get-user-id crux-node "es"))

  (add-lembrando crux-node uid "que?" "pois")

  ;; throws "unknown user"
  (add-lembrando crux-node (uuid/v1) "ha?" "já")
  (require '[crux.api :as crux])
  (require '[relembra.crux :as rc])

  (def lid
    (rc/q1 crux-node
          {:find ['lid]
           :where [['lid :relembra.lembrando/user]]}))

  (def e (crux/entity (crux/db crux-node) lid))

  (lembrando? e)

  )
