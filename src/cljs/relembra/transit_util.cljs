(ns relembra.transit-util
  (:require
   [ajax.core :as ajax]
   [cognitect.transit :as t]))


(def transit-ajax-response-format
  (ajax/transit-response-format
   ;; https://github.com/cognitect/transit-cljs/pull/10
   {:reader (t/reader :json {:handlers {"u" uuid}})}))
