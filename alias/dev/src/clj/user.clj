(ns user
  (:require
   [clojure.tools.namespace.repl :as nsrepl]
   [integrant.repl :refer (go reset halt)]
   [relembra.system :as sys]))


(sys/init-logging :debug)
(nsrepl/set-refresh-dirs "alias/dev/src/clj" "alias/dev/src/cljc" "src/clj" "src/cljc")
(sys/prep (sys/load-config "dev-config.clj"))

(comment
  ;; start system like this
  (go)
  ;; soft restart system like this
  (reset)
  ;; hard stop system like this
  (halt)


  (def crux-node (:relembra.crux/node integrant.repl.state/system))

  (require '[relembra.db-model.user :as db-user])

  (db-user/get-user-id crux-node "es")

  )
