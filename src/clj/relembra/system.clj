(ns relembra.system
  (:require [buddy.hashers :as hashers]
            [clojure.java.io :as io]
            [hashp.core]                ; to enable #p data readers
            [integrant.core :as ig]
            integrant.repl
            [relembra.db-model.user :as db-user]
            [taoensso.timbre :as timbre :refer [info warn]]
            [taoensso.timbre.tools.logging :refer [use-timbre]]))


(defn init-logging [level]
  (add-tap (bound-fn* clojure.pprint/pprint))
  (use-timbre)
  (timbre/swap-config!
   assoc :ns-log-level
   [["relembra.*" level]
    ["*" :info]]))


(defn hotload-dep
  "Only works with -A:hotload-deps"
  [& args]
  (if-let [f
           (try (requiring-resolve 'clojure.tools.deps.alpha.repl/add-lib)
                (catch java.io.FileNotFoundException _ nil))]
    (apply f args)
    (warn "dep hotloading is disabled; enable with -A:hotload-deps")))


(defn load-config [filename]
  (read-string (slurp (io/file filename))))


(defmethod ig/init-key ::init [_ {:keys [crux-node]
                                  {:keys [username password]} :user}]
  (db-user/add-user crux-node username (hashers/derive password)))


(defn prep [{:keys [db-dir init-user http-port nrepl-port]}]
  (let [system-cfg
        (cond-> {:relembra.crux/node {:dir db-dir}
                 :relembra.ring-handler/handler
                 {:crux-node (ig/ref :relembra.crux/node)}
                 :relembra.jetty/server
                 {:port http-port
                  :handler (ig/ref :relembra.ring-handler/handler)}}
          nrepl-port (assoc :relembra.nrepl/server {:port nrepl-port})
          init-user (assoc :relembra.system/init {:crux-node (ig/ref :relembra.crux/node)
                                                  :user init-user}))]
    (info "System config:" (pr-str system-cfg))
    (ig/load-namespaces system-cfg)
    (integrant.repl/set-prep! (constantly system-cfg))))


(comment


  )
