(ns user
  (:require
   [integrant.repl :refer (go reset halt)]
   [relembra.system :as sys]))


(sys/init-logging :info)
(sys/restrict-refresh-dirs ["alias/prod" "src"])
(sys/prep (sys/load-config "prod-config.clj"))
(go)


(comment
  ;; start system like this
  (go)
  ;; soft restart system like this
  (reset)
  ;; hard stop system like this
  (halt)

  )
