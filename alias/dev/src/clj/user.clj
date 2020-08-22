(ns user
  (:require
   [integrant.repl :refer (go reset halt)]
   [relembra.system :as sys]))


(sys/init-logging :debug)
(sys/restrict-refresh-dirs ["alias/dev" "src"])
(sys/prep (sys/load-config "dev-config.clj"))

(comment
  ;; start system like this
  (go)
  ;; soft restart system like this
  (reset)
  ;; hard stop system like this
  (halt)


  )
