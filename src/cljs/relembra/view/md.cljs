(ns relembra.view.md
  (:require [markdown.core :refer [md->html]]
            [re-frame.core :as rf]
            [reagent.dom :as rd]))


(rf/reg-event-db
 :editor/set-value
 (fn [db [_ val]]
   (assoc db :editor/value val)))


(rf/reg-sub
 :editor/value
 (fn [db _]
   (:editor/value db)))

(defn typeset [c]
  (js/MathJax.Hub.Queue (array "Typeset" js/MathJax.Hub (rd/dom-node c))))

(defn markdown-box [text]
  [:div {:style {:font-family "Yrsa, serif" :font-size "120%"}
         :dangerouslySetInnerHTML {:__html (md->html text :inhibit-separator "$")}}])

(def mathjax-box
  (with-meta markdown-box
    {:component-did-mount typeset
     :component-did-update typeset}))

(defn md []
  (let [val @(rf/subscribe [:editor/value])]
    [:div
     [:textarea
      {:value val
       :on-change (fn [e] (rf/dispatch [:editor/set-value (-> e .-target .-value)]))}]
     [mathjax-box val]]))
