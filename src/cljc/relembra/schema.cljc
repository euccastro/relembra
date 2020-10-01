(ns relembra.schema
  (:require [malli.core :as m]
            [malli.registry :as mr]
            [tick.alpha.api :as t :refer [date?]]))


(def registry
  (let [schema (m/-simple-schema {:type 'date? :pred date?})]
    (mr/registry
     (-> (m/default-schemas)
         (assoc 'date? schema)
         (assoc date? schema)))))


(def nonempty-string [:string {:min 1}])


(def user
  [:map
   [:crux.db/id uuid?]
   [:relembra.user/name nonempty-string]
   [:relembra.user/hashed-password nonempty-string]
   [:relembra.user/of-matrix {:optional true} [:vector double?]]])
(def user-validator (m/validator user))


(def question
  [:map
   [:crux.db/id uuid?]
   [:relembra.question/question nonempty-string]
   [:relembra.question/answer nonempty-string]])
(def question-validator (m/validator question))


(def lembrando
  [:map
   [:crux.db/id uuid?]
   [:relembra.lembrando/user uuid?]
   [:relembra.lembrando/question uuid?]
   [:relembra.lembrando/due-date date?]
   [:relembra.lembrando/failing? {:optional true} boolean?]])
(def lembrando-validator
  (m/validator lembrando {:registry registry}))


(def recall
  [:map
   [:crux.db/id uuid?]
   [:relembra.recall/user uuid?]
   [:relembra.recall/lembrando uuid?]
   [:relembra.recall/rate pos-int?]])
(def recall-validator (m/validator recall))


(comment


  (require '[clj-uuid :as uuid])

  (user-validator {:crux.db/id (uuid/v1)
                   :relembra.user/name "es"
                   :relembra.user/hashed-password "abcde123"
                   :relembra.user/of-matrix [1.0 2.0 3.0]})

  (lembrando-validator {:crux.db/id (uuid/v1)
                        :relembra.lembrando/user (uuid/v1)
                        :relembra.lembrando/question (uuid/v1)
                        :relembra.lembrando/due-date (t/date)
                        :relembra.lembrando/failing? true})

  (recall-validator {:crux.db/id (uuid/v1)
                     :relembra.recall/user (uuid/v1)
                     :relembra.recall/lembrando (uuid/v1)
                     :relembra.recall/rate 3})
  )
