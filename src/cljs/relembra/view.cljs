(ns relembra.view
  (:require
   [kee-frame.core :as kf]
   [relembra.view.edit-question :refer [edit-question]]
   [relembra.view.md :refer [md]]
   [relembra.view.review :refer [review]]
   [re-frame.core :as rf]))


(def page-names
  [[:review "Review"]
   [:md "MD"]
   [:edit-question "Edit question"]])


(defn not-found []
  [:div
   [:h1 "WAT"]
   [:p "Not found, 404, etc."]])


(defn navigation [current-page]
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
        error @(rf/subscribe [:common/error])]
    [:div
     (when error
       [:div [:pre [:code {:style {:color :red}}
                    (with-out-str (cljs.pprint/pprint error))]]])
     [navigation current-page]
     [:main
      (case current-page
        :review [review]
        :md [md]
        :edit-question [edit-question]
        :not-found [not-found]
        [:div "Loading..."])]]))
