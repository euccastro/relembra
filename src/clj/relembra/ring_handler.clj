(ns relembra.ring-handler
  (:require [hiccup.core :refer (html)]
            [hiccup.page :refer (doctype)]
            [integrant.core :as ig]
            [muuntaja.middleware :refer [wrap-format]]
            [reitit.ring :as rring]
            [ring.middleware.anti-forgery :refer (*anti-forgery-token*
                                                  wrap-anti-forgery)]
            [ring.middleware.params :refer (wrap-params)]
            [ring.middleware.keyword-params :refer (wrap-keyword-params)]
            [ring.middleware.session :refer (wrap-session)]
            [ring.util.http-response :refer (content-type ok)]))


(defn- frontpage [_]
  (-> (html
       (doctype :html5)
       [:html
        [:head
         [:link {:rel "stylesheet" :href "css/mvp.css" :type "text/css"}]
         [:meta {:charset "UTF-8"}]
         [:meta {:name "viewport"
                 :content "width=device-width, initial-scale=1.0"}]
         [:title "Relembra"]]
        [:body
         [:div#app]
         [:script {:type "text/javascript"}
          (str "var csrfToken = \""
               *anti-forgery-token*
               "\";")]
         [:script {:type "text/javascript"
                   :src "/js/relembra.js"}]]])
      ok
      (content-type "text/html; charset=utf-8")))


(defn- handler [crux-node]
  (rring/ring-handler
   (rring/router
    [["/test" {:get (constantly {:status 200
                                 :headers {"Content-Type" "text/plain"}
                                 :body "OK"})}]])
   (rring/routes
    (rring/create-resource-handler
       {:path "/"})
    (rring/create-default-handler
     {:not-found frontpage}))))


(defmethod ig/init-key ::handler
  [_ {:keys [crux-node]}]
  (-> (handler crux-node)
      (wrap-anti-forgery
       {:read-token (fn [req]
                      (get-in req [:body-params :csrf-token]))})
      wrap-session
      wrap-format
      wrap-keyword-params
      wrap-params))
