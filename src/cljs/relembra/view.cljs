(ns relembra.view
  (:require
   [kee-frame.core :as kf]
   [relembra.view.review :refer [review]]
   [relembra.view.edit-question :refer [edit-question]]
   [re-frame.core :as rf]))


(def page-names
  [[:review "Review"]
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
         page-name
         [:a {:href (kf/path-for [page-id])}
          page-name])])]])


(defn root []
  (let [current-page @(rf/subscribe [:nav/page])]
    [:div
     [navigation current-page]
     [:main
      (case current-page
        :review [review]
        :edit-question [edit-question]
        :not-found [not-found]
        [:div "Loading..."])]]))
