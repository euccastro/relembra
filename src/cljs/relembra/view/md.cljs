(ns relembra.view.md
  (:require [re-frame.core :as rf]
            [markdown.core :refer [md->html]]))


(rf/reg-event-db
 :editor/set-value
 (fn [db [_ val]]
   (assoc db :editor/value val)))


(rf/reg-sub
 :editor/value
 (fn [db _]
   (:editor/value db)))


(defn md []
  (let [val @(rf/subscribe [:editor/value])]
    [:div
     [:textarea
      {:value val
       :on-change (fn [e] (rf/dispatch [:editor/set-value (-> e .-target .-value)]))}]
     [:div {:style {:font-family "Yrsa, serif" :font-size "120%"}
            :dangerouslySetInnerHTML {:__html (md->html val)}}]]))
