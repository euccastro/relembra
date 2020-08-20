(ns relembra.ring-handler
  (:require [buddy.auth :refer [authenticated?]]
            [buddy.auth.accessrules :refer [restrict]]
            [buddy.auth.backends.session :refer [session-backend]]
            [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
            [hiccup.core :refer [html]]
            [hiccup.page :refer [doctype]]
            [integrant.core :as ig]
            [muuntaja.middleware :refer [wrap-format]]
            [reitit.ring :as rring]
            [ring.middleware.anti-forgery :refer [*anti-forgery-token*
                                                  wrap-anti-forgery]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.util.http-response :refer [content-type ok see-other]]))


(defn- html5-ok [title body]
  (-> (html
       (doctype :html5)
       [:html
        [:head
         ;[:link {:rel "stylesheet" :href "css/mvp.css" :type "text/css"}]
         [:meta {:charset "UTF-8"}]
         [:meta {:name "viewport"
                 :content "width=device-width, initial-scale=1.0"}]
         [:title title]]
        body])
      ok
      (content-type "text/html; charset=utf-8")))


(defn- frontpage [_]
  (html5-ok "Relembra"
            [:body
             [:div#app]
             [:script {:type "text/javascript"}
              (str "var csrfToken = \""
                   *anti-forgery-token*
                   "\";")]
             [:script {:type "text/javascript"
                       :src "/js/relembra.js"}]]))

(defn- login [redirect-to]
  (html5-ok "Login"
   [:body
    [:form {:method "post"
            :action "/login"}
     [:input {:type "hidden" :name "csrf-token" :value *anti-forgery-token*}]
     [:input {:type "hidden" :name "redirect-to" :value redirect-to}]
     [:div
      [:label "Name"
       [:input {:type "text" :name "name" :required true}]]]
     [:div
      [:label "Password"
       [:input {:type "password" :name "password" :required true}]]]
     [:div
      [:input {:type "submit" :value "Login"}]]]]))


(defn on-error [request _]
  (->
   {:status 403
    :body (str "Access to " (:uri request) " is not authorized")}
   (content-type "text/plain; charset=utf-8")))


(defn wrap-restricted [handler]
  (restrict handler {:handler authenticated?
                     :on-error on-error}))


(defn wrap-auth [handler]
  (let [backend (session-backend)]
    (-> handler
        (wrap-authentication backend)
        (wrap-authorization backend))))


(defn handle-login [{{:keys [name password redirect-to]} :params
                     :as req}]
  (if (= [name password] ["es" "quod"])
    (-> (see-other redirect-to)
        (assoc :session (assoc (:session req) :identity "es")))
    ;; XXX: set error string in login instead
    (on-error {:uri redirect-to} nil)))


(defn- handler [crux-node]
  (rring/ring-handler
   (rring/router
    [["/login" {:get #(-> % :params :redirect-to login)
                :post handle-login}]
     ["/test" {:middleware [wrap-restricted]
               :get (constantly {:status 200
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
      wrap-auth
      (wrap-anti-forgery
       {:read-token (fn [req]
                      (get-in req [:params :csrf-token]))})
      wrap-session
      wrap-format
      wrap-keyword-params
      wrap-params))
