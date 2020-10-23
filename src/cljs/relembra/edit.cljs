(ns relembra.edit
  (:require [markdown.core :refer [md->html]]
            [re-frame.core :as rf]
            [reagent.dom :as rd]))


(rf/reg-event-db
 :edit/set-body
 (fn [db [_ id val]]
   (assoc db id val)))


(rf/reg-sub
 :edit/question-body
 (fn [db _]
   (:edit/question-body db)))


(rf/reg-sub
 :edit/answer-body
 (fn [db _]
   (:edit/answer-body db)))


(defn typeset [c]
  (js/MathJax.Hub.Queue (array "Typeset" js/MathJax.Hub (rd/dom-node c))))


(defn markdown-box [text]
  [:div {:style {:font-family "Yrsa, serif" :font-size "120%"}
         :dangerouslySetInnerHTML {:__html (md->html text :inhibit-separator "$")}}])


(def mathjax-box
  (with-meta markdown-box
    {:component-did-mount typeset
     :component-did-update typeset}))


(defn qa-pair [id val]
  [:div
   {:style {:display "flex"
            :justify-content "space-between"}}
   [:textarea
    {:value val
     :on-change (fn [e] (rf/dispatch [:edit/set-body id (-> e .-target .-value)]))}]
   [mathjax-box val]])


(defn edit []
  [:div
   {:style {:padding "40px"}}
   (doall
    (for [id [:edit/question-body :edit/answer-body]]
      ^{:key id} [qa-pair id @(rf/subscribe [id])]))])
