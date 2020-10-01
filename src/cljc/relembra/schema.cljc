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
   [:name nonempty-string]
   [:hashed-password nonempty-string]
   [:of-matrix {:optional true} [:vector double?]]])
(def user-validator (m/validator user))


(def question
  [:map
   [:crux.db/id uuid?]
   [:question nonempty-string]
   [:answer nonempty-string]])
(def question-validator (m/validator question))


(def lembrando
  [:map
   [:crux.db/id uuid?]
   [:user uuid?]
   [:question uuid?]
   [:due-date date?]
   [:failing? {:optional true} boolean?]])
(def lembrando-validator
  (m/validator lembrando {:registry registry}))


(comment


  (require '[clj-uuid :as uuid])

  (user-validator {:crux.db/id (uuid/v1) :name "es" :hashed-password ""})

  (m/validate user {:crux.db/id (uuid/v1) :name "es" :hashed-password "abcde123"})
  (m/validate user {:crux.db/id (uuid/v1) :name "es" :hashed-password "abcde123" :of-matrix [1.0 2.0 3.0]})

  (lembrando-validator {:crux.db/id (uuid/v1)
                        :user (uuid/v1)
                        :question (uuid/v1)
                        :due-date (t/date)
                        :failing? true})

  )
