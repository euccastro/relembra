(ns relembra.jetty
  (:require
   [integrant.core :as ig]
   [ring.adapter.jetty :as jetty])
  (:import org.eclipse.jetty.server.Server))


(defmethod ig/init-key ::server [_ {:keys [port handler]}]
  (let [port (or port 62000)
        ;; to support suspend/resume; see the following for the general idea:
        ;; https://github.com/weavejester/integrant#suspending-and-resuming
        wrapped-handler (atom (delay handler))
        server
        (jetty/run-jetty (fn [req] (@@wrapped-handler req)) {:port port :join? false})]
    {:server server
     :handler wrapped-handler}))


(defmethod ig/halt-key! ::server [_ {:keys [server]}]
  (.stop ^Server server))


(defmethod ig/suspend-key! ::server [_ {:keys [handler]}]
  (reset! handler (promise)))


(defmethod ig/resume-key ::server [key opts old-opts old-impl]
  (if (= (dissoc opts :handler) (dissoc old-opts :handler))
    ;; same server config, just update the handler
    (do (deliver @(:handler old-impl) (:handler opts))
        old-impl)
    ;; server config changed; restart it altogether
    (do (ig/halt-key! key old-impl)
        (ig/init-key key opts))))
