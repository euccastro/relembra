(ns relembra.db-model.lembrando
  (:require [clj-uuid :as uuid]
            [relembra.crux :refer [q1 sync-tx]]
            [relembra.db-model.qa :as db-qa]
            [relembra.schema :refer [lembrando?]]
            [tick.alpha.api :as t]
            [crux.api :as crux]))


(defn qa->lembrando-id [crux-node qa]
  (q1 crux-node
      {:find ['l]
       :where [['l :relembra.lembrando/qa qa]]}))


(defn update-lembrando [crux-node qa old new]
  (db-qa/check-qa crux-node qa)
  (let [lid (or (qa->lembrando-id crux-node qa) (uuid/v1))
        lembrando (assoc new
                         :crux.db/id lid
                         :relembra.lembrando/qa qa)]
    (cond
      (not (lembrando? lembrando))
      (throw (ex-info "invalid lembrando?" {:lembrando lembrando}))
      :else (and (sync-tx crux-node
                          [[:crux.tx/match lid old]
                           [:crux.tx/put lembrando]])
                 lid))))


(comment

  (do
    (require '[integrant.repl.state :refer [system]])
    (require '[relembra.db-model.user :as db-user])
    (def crux-node (:relembra.crux/node system)))

  (def uid (db-user/get-user-id crux-node "es"))
  (def qa (db-qa/add-qa crux-node uid "que?" "pois"))
  (def l {:relembra.lembrando/due-date (t/date)
          :relembra.lembrando/failing? true
          :relembra.lembrando/remembering-state [1.0 2.0]})
  (def lid (update-lembrando crux-node qa nil
                             l))

  (def l2 (assoc l :crux.db/id lid :relembra.lembrando/qa qa))

  (update-lembrando crux-node qa l2 (assoc l2 :relembra.lembrando/failing? false))

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
