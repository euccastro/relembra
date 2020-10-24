(ns relembra.db-model.qa
  (:require [clj-uuid :as uuid]
            [relembra.crux :refer [q1 sync-tx]]
            [relembra.db-model.user :as db-user]
            [relembra.schema :refer [qa?]]
            [crux.api :as crux]
            [better-cond.core :as b]))


(defn existing-question [crux-node user-id question]
  (q1 crux-node
      {:find ['qa]
       :where [['qa :relembra.qa/owner user-id]
               ['qa :relembra.qa/question question]]}))


;; XXX: allow editing existing qa
(defn add-qa [crux-node user-id question answer]
  (let [id (uuid/v1)
        qa {:crux.db/id id
            :relembra.qa/owner user-id
            :relembra.qa/question question
            :relembra.qa/answer answer}]
    (b/cond
      (not (qa? qa))
      (throw (ex-info "invalid question/answer pair?" {:qa qa}))
      (not (db-user/user-exists? crux-node user-id))
      (throw (ex-info "unknown user?" {:user-id user-id}))
      :let [eq (existing-question crux-node user-id question)]
      eq
      (throw (ex-info "question exists for user"
                      {:question question
                       :existing-qa eq
                       :user-id user-id}))
      :else (and (sync-tx crux-node
                          [[:crux.tx/match id nil]
                           [:crux.tx/put qa]])
                 id))))


(defn edit-qa [crux-node
               old
               {:keys [relembra.qa/owner
                       relembra.qa/question
                       crux.db/id] :as new}]
  (let [eq (existing-question crux-node owner question)]
    (cond
      (not (qa? old))
      (throw (ex-info "bad old question/answer pair:"
                      {:old-qa old}))
      (not (qa? new))
      (throw (ex-info "bad new question/answer pair:"
                      {:new-qa new}))
      (and eq (not= eq id))
      (throw (ex-info "question exists for user"
                      {:question question
                       :existing-qa eq
                       :user-id owner}))
      :else
      (sync-tx crux-node
               [[:crux.tx/match id old]
                [:crux.tx/put new]]))))


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

  (def ent (crux/entity (crux/db crux-node) :not-there))
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

  (type (ex-info "blah" {:x 1}))

  clojure.lang.ExceptionInfo

  (qa-exists? crux-node id)
  (qa-exists? crux-node (uuid/v1))
  )
