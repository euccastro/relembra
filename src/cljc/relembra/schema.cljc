(ns relembra.schema
  (:require [malli.core :as m]))

(def nonempty-string [:string {:min 1}])

(def user-src
  [:map
   [:crux.db/id uuid?]
   [:name nonempty-string]
   [:hashed-password nonempty-string]
   [:of-matrix {:optional true} [:vector double?]]])

(def user-validator (m/validator user-src))


(comment


  (require '[clj-uuid :as uuid])

  (user-validator {:crux.db/id (uuid/v1) :name "es" :hashed-password ""})

  (m/validate user-src {:crux.db/id (uuid/v1) :name "es" :hashed-password "abcde123"})
  (m/validate user-src {:crux.db/id (uuid/v1) :name "es" :hashed-password "abcde123" :of-matrix [1.0 2.0 3.0]})

  )
