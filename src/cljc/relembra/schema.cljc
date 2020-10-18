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
(def user? (m/validator user))


(def qa
  [:map
   [:crux.db/id uuid?]
   [:relembra.qa/owner uuid?]
   [:relembra.qa/question nonempty-string]
   [:relembra.qa/answer nonempty-string]])
(def qa? (m/validator qa))


(def lembrando
  [:map
   [:crux.db/id uuid?]
   [:relembra.lembrando/qa uuid?]
   [:relembra.lembrando/due-date date?]
   [:relembra.lembrando/failing? boolean?]
   [:relembra.lembrando/remembering-state [:vector double?]]])
(def lembrando?
  (m/validator lembrando {:registry registry}))


(def recall
  [:map
   [:crux.db/id uuid?]
   [:relembra.recall/user uuid?]
   [:relembra.recall/lembrando uuid?]
   [:relembra.recall/rate pos-int?]])
(def recall? (m/validator recall))


(comment

  (require '[clj-uuid :as uuid])

  (user? {:crux.db/id (uuid/v1)
          :relembra.user/name "es"
          :relembra.user/hashed-password "abcde123"
          :relembra.user/of-matrix [1.0 2.0 3.0]})

  (lembrando? {:crux.db/id (uuid/v1)
               :relembra.lembrando/qa (uuid/v1)
               :relembra.lembrando/due-date (t/date)
               :relembra.lembrando/failing? true
               :relembra.lembrando/remembering-state [1.0 2.0 3.0]})

  (recall? {:crux.db/id (uuid/v1)
            :relembra.recall/user (uuid/v1)
            :relembra.recall/lembrando (uuid/v1)
            :relembra.recall/rate 3})
  )
