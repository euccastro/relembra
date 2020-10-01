(ns relembra.ring-handler
  (:require [better-cond.core :as b]
            [buddy.auth :refer [authenticated?]]
            [buddy.auth.accessrules :refer [restrict]]
            [buddy.auth.backends.session :refer [session-backend]]
            [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
            [buddy.hashers :as hashers]
            [hiccup.core :refer [html]]
            [hiccup.page :refer [doctype]]
            [integrant.core :as ig]
            [muuntaja.middleware :refer [wrap-format]]
            [reitit.ring :as rring]
            [relembra.db-model.user :as db-user]
            [ring.middleware.anti-forgery :refer [*anti-forgery-token*
                                                  wrap-anti-forgery]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.util.http-response :refer [content-type ok see-other]]))


(defn- html5-ok
  ([title body]
   (html5-ok title [] body))
  ([title head body]
   (-> (html
        (doctype :html5)
        `[:html
          [:head
           [:meta {:charset "UTF-8"}]
           [:meta {:name "viewport"
                   :content "width=device-width, initial-scale=1.0"}]
           [:title ~title]
           ~@head]
          [:body
           ~@body]])
       ok
       (content-type "text/html; charset=utf-8"))))

(defn- frontpage [_]
  (html5-ok "Relembra"
            [[:link {:rel "stylesheet" :href "https://cdn.jsdelivr.net/font-hack/2.020/css/hack-extended.min.css"}]
             [:link {:rel "stylesheet" :href "https://fonts.googleapis.com/css?family=Yrsa"}]
             [:link {:rel "stylesheet" :href "https://fonts.googleapis.com/css?family=Roboto:400,300,500&amp;subset=latin" :media "all"}]
             [:script {:type "text/x-mathjax-config"}
              "MathJax.Hub.Config({asciimath2jax: {delimiters: [['¡','¡']]}});"]
             [:script#MathJax-script
              {:type "text/javascript"
               :async true
               :src "https://cdn.jsdelivr.net/npm/mathjax@2/MathJax.js?config=AM_CHTML"}]]
            [[:div#app]
             [:script {:type "text/javascript"}
              (str "var csrfToken = \""
                   *anti-forgery-token*
                   "\";")]
             [:script {:type "text/javascript"
                       :src "/js/relembra.js"}]]))

(defn- login [redirect-to]
  (html5-ok "Login"
            [[:form {:method "post"
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


(defn- credentials->user [crux-node user-name password]
  (b/cond
    :when-let [user-id (db-user/get-user-id crux-node user-name)
               hashed-password (db-user/get-hashed-password crux-node user-id)]
    :when (hashers/check password hashed-password)
    user-id))


(defn login-handler [crux-node]
  (fn handle-login [{{:keys [name password redirect-to]} :params
                     :as req}]
    (if-let [user-id (credentials->user crux-node name password)]
      (-> (see-other redirect-to)
          (assoc :session (assoc (:session req) :identity user-id)))
      ;; XXX: set error string in login instead
      (on-error {:uri redirect-to} nil))))


(defn- handler [crux-node]
  (rring/ring-handler
   (rring/router
    [["/login" {:get #(-> % :params :redirect-to login)
                :post (login-handler crux-node)}]
     [""
      {:middleware [wrap-restricted]}
      ["/initial-data" {:get (constantly (ok {:test/data 42}))}]
      ["/test" {:get (constantly {:status 200
                                  :headers {"Content-Type" "text/plain"}
                                  :body "OK"})}]]])
   (identity #_wrap-restricted
    (rring/routes
     (rring/create-resource-handler
      {:path "/"})
     (rring/create-default-handler
      {:not-found frontpage})))))


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


(comment

  (do
    (require '[integrant.repl.state :refer [system]])
    (def crux-node (:relembra.crux/node system)))

  (defn add-user [crux-node name password]
    (db-user/add-user crux-node name (hashers/derive password)))

  (add-user crux-node "outro" "senha")

  )
