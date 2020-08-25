(ns relembra.nrepl
  [:require
   [integrant.core :as ig]
   [nrepl.server :refer (start-server stop-server)]
   [taoensso.timbre :refer [info]]])


(defmethod ig/init-key ::server [_ {:keys [port]}]
  (info "Starting nREPL server at port" port "...")
  (start-server :bind "127.0.0.1" :port port))


(defmethod ig/halt-key! ::server [_ server]
  (stop-server server))


(defmethod ig/suspend-key! ::server [& _]
  nil)


(defmethod ig/resume-key ::server [& _]
  nil)
