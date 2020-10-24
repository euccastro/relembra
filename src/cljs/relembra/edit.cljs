(ns relembra.edit
  (:require [kee-frame.core :as kf]
            [markdown.core :refer [md->html]]
            [re-frame.core :as rf]
            [reagent.dom :as rd]
            [relembra.transit-util :refer (transit-ajax-request-format
                                           transit-ajax-response-format)]))


(rf/reg-event-db
 :edit/set-body
 (fn [db [_ id val]]
   (assoc db id val)))


(rf/reg-event-db
 :edit/clear
 (fn [db _]
   (dissoc db :edit/question-body :edit/answer-body)))


(rf/reg-event-db
 :edit/swap
 (fn [{:keys [edit/question-body edit/answer-body] :as db} _]
   (assoc db
          :edit/question-body answer-body
          :edit/answer-body question-body)))


(rf/reg-event-fx
 :edit/new
 (fn [& _]
   {:navigate-to [:edit {:qa :new}]}))


(rf/reg-event-fx
 :edit/save
 (fn [& _]
   {:navigate-to [:edit {:qa (random-uuid)}]}))


(defn- update-qa [db qa]
  (assoc-in db [:qa (:crux.db/id qa)]
            (select-keys qa [:crux.db/id :relembra.qa/question :relembra.qa/answer])))


(kf/reg-chain
 :edit/add
 (fn [_ [params]]
   {:http-xhrio {:method :post
                 :uri "/qa"
                 :params (assoc params :csrf-token js/csrfToken)
                 :format transit-ajax-request-format
                 :response-format transit-ajax-response-format
                 :on-failure [:common/set-error]}})
 (fn [{:keys [db]} [_ {:keys [crux.db/id] :as ret}]]
   {:db (update-qa db ret)
    :navigate-to [:edit {:qa id}]}))


(kf/reg-chain
 :edit/update
 (fn [_ [params]]
   {:http-xhrio {:method :put
                 :uri "/qa"
                 :params (assoc params :csrf-token js/csrfToken)
                 :format transit-ajax-request-format
                 :response-format transit-ajax-response-format
                 :on-failure [:common/set-error]}})
 (fn [{:keys [db]} [_ {:keys [status new]}]]
   {:db
    (if (= status :success)
      (update-qa db new)
      (assoc db :common/set-error "Should refresh now!"))}))


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


(defn edit [qa-id]
  (let [q @(rf/subscribe [:db/path [:edit/question-body]])
        a @(rf/subscribe [:db/path [:edit/answer-body]])
        saved-qa (and qa-id @(rf/subscribe [:db/path [:qa qa-id]]))]
    [:div
     {:style {:padding "40px"}}
     (doall
      (for [[id body] [[:edit/question-body q]
                       [:edit/answer-body a]]]
        ^{:key id} [qa-pair id body]))
     [:div
      {:style {:display :flex}}
      [:button {:on-click #(rf/dispatch [:edit/clear])}
       "Clear"]
      [:button {:on-click #(rf/dispatch [:edit/swap])}
       "Swap"]
      [:button {:on-click #(rf/dispatch [:edit/new])}
       "New"]
      [:button {:on-click #(rf/dispatch
                            (let [new-qa {:crux.db/id qa-id
                                          :relembra.qa/question q
                                          :relembra.qa/answer a}]
                              (if qa-id
                                [:edit/update
                                 {:old saved-qa :new new-qa}]
                                [:edit/add (dissoc new-qa :crux.db/id)])))}
       "Save"]]]))
