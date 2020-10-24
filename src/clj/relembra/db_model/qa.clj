(ns relembra.db-model.qa
  (:require [clj-uuid :as uuid]
            [relembra.crux :refer [q1 sync-tx]]
            [relembra.db-model.user :as db-user]
            [relembra.schema :refer [qa?]]
            [crux.api :as crux]))


(defn existing-question? [crux-node user-id question]
  (some? (q1 crux-node
             {:find ['qa]
              :where [['qa :relembra.qa/owner user-id]
                      ['qa :relembra.qa/question question]]})))


;; XXX: allow editing existing qa
(defn add-qa [crux-node user-id question answer]
  (let [id (uuid/v1)
        qa {:crux.db/id id
            :relembra.qa/owner user-id
            :relembra.qa/question question
            :relembra.qa/answer answer}]
    (cond
      (not (qa? qa))
      (throw (ex-info "invalid question/answer pair?" {:qa qa}))
      (not (db-user/user-exists? crux-node user-id))
      (throw (ex-info "unknown user?" {:user-id user-id}))
      (existing-question? crux-node user-id question)
      (throw (ex-info "question exists for user"
                      {:question question
                       :user-id user-id}))
      :else (and (sync-tx crux-node
                          [[:crux.tx/match id nil]
                           [:crux.tx/put qa]])
                 id))))


(defn qa-exists? [crux-node qa-id]
  (some?
   (q1 crux-node
       {:find ['o]
        :where [[qa-id :relembra.qa/owner 'o]]})))


(defn check-qa [crux-node qa]
  (when-not (qa-exists? crux-node qa)
    (throw (ex-info "unknown question/answer pair?" {:qa qa}))))


(comment

  (do
    (require '[integrant.repl.state :refer [system]])
    (def crux-node (:relembra.crux/node system)))

  (def uid (db-user/get-user-id crux-node "es"))

  (add-qa crux-node uid "que?" "pois")

  ;; throws "unknown user"
  (add-qa crux-node (uuid/v1) "ha?" "já")

  (require '[crux.api :as crux])
  (require '[relembra.crux :as rc])

  (def id
    (rc/q1 crux-node
          {:find ['id]
           :where [['id :relembra.qa/owner]]}))

  (def e (crux/entity (crux/db crux-node) id))

  (qa? e)

  (qa-exists? crux-node id)
  (qa-exists? crux-node (uuid/v1))
  )
