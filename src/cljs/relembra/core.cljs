(ns ^:figwheel-hooks relembra.core
  (:require
   [kee-frame.core :as kf]
   [relembra.transit-util :refer (transit-ajax-response-format)]
   [relembra.edit :refer [edit]]
   [relembra.review :refer [review]]
   [re-frame.core :as rf]))


;; XXX: handle failure
(kf/reg-chain
 ::load-initial-data
 (fn [_ _]
   {:http-xhrio {:method          :get
                 :uri             "/initial-data"
                 :response-format transit-ajax-response-format
                 :on-failure      [:common/set-error]}})
 (fn [{:keys [db]} [_ initial-data]]
   {:db (merge db initial-data)}))


(kf/reg-controller
 ::load-initial-data-on-startup
 {:params (constantly true)
  :start  [::load-initial-data]})


(rf/reg-sub
 :nav/page
 :<- [:kee-frame/route]
 (fn [route _]
   (-> route :data :name)))


(rf/reg-event-db
 :common/set-error
 (fn [db error]
   (assoc db :common/error error)))


(rf/reg-sub
 :top-level-key
 (fn [db [_ k]]
   (k db)))


(def routes
  [["/" :review]
   ["/edit/:qa" :edit]
   ["/not-found" :not-found]])


(def page-names
  [[:review "Review"]
   [:edit "Edit question"]])


(defn not-found []
  [:div
   [:h1 "WAT"]
   [:p "Not found, 404, etc."]])


(defn navigation [current-page qa]
  [:nav
   [:ul
    (for [[page-id page-name] page-names]
      ^{:key (name page-id)}
      [:li
       (if (= current-page page-id)
         [:strong page-name]
         [:a {:href (kf/path-for [page-id])}
          page-name])])]])


(defn root []
  (let [current-page @(rf/subscribe [:nav/page])
        error @(rf/subscribe [:top-level-key :common/error])]
    [:div
     (when error
       [:div [:pre [:code {:style {:color :red}}
                    (with-out-str (cljs.pprint/pprint error))]]])
     [navigation current-page]
     [:main
      (case current-page
        :review [review]
        :edit [edit]
        :not-found [not-found]
        [:div "Loading..."])]]))

(defn ^:after-load mount!
  ([] (mount! true))
  ([debug?]
   (rf/clear-subscription-cache!)
   (kf/start! {:debug? (boolean debug?)
               :routes routes
               :not-found "/not-found"
               :initial-db {}
               :root-component [root]})))


(defn init! [debug?]
  ;; one-off (not reloaded) initialization would go here.
  (mount! debug?))
