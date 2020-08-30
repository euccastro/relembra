(ns user
  (:require
   [clojure.tools.namespace.repl :as nsrepl]
   [integrant.repl :refer (go reset halt)]
   [relembra.system :as sys]))


(sys/init-logging :info)
(nsrepl/set-refresh-dirs ["alias/prod/src/clj" "alias/prod/src/cljc" "src/clj" "src/cljc"])
(sys/prep (sys/load-config "prod-config.clj"))


(comment
  ;; start system like this
  (go)
  ;; soft restart system like this
  (reset)
  ;; hard stop system like this
  (halt)

  )
