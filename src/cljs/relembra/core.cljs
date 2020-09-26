(ns ^:figwheel-hooks relembra.core
  (:require
   [kee-frame.core :as kf]
   [relembra.transit-util :refer (transit-ajax-response-format)]
   [relembra.view :as view]
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
 :common/error
 (fn [db _]
   (:common/error db)))


(def routes
  [["/" :review]
   ["/edit-question" :edit-question]
   ["/md" :md]
   ["/not-found" :not-found]])


(defn ^:after-load mount!
  ([] (mount! true))
  ([debug?]
   (rf/clear-subscription-cache!)
   (kf/start! {:debug? (boolean debug?)
               :routes routes
               :not-found "/not-found"
               :initial-db {}
               :root-component [view/root]})))


(defn init! [debug?]
  ;; one-off (not reloaded) initialization would go here.
  (mount! debug?))
