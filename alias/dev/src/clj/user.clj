(ns user
  (:require
   [clojure.pprint]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.tools.namespace.repl :as nsrepl]
   [crux.api :as crux]
   [hashp.core]                         ; to enable #p data readers
   [integrant.core :as ig]
   [integrant.repl :refer (go reset resume halt)]
   [integrant.repl.state :refer (system)]
   [taoensso.timbre :as timbre
    :refer [log  trace  debug  info  warn  error  fatal  report
            logf tracef debugf infof warnf errorf fatalf reportf
            spy get-env]]
   [taoensso.timbre.tools.logging :refer (use-timbre)]))


;; logging
(add-tap (bound-fn* clojure.pprint/pprint))
(use-timbre)
(timbre/swap-config!
 assoc :ns-log-level
 [["relembra.*" :debug]
  ["*" :info]])

;; integrant.repl/reset will cause havoc with hot-reloading and figwheel-main
;; unless we restrict these.
(apply nsrepl/set-refresh-dirs
       (for [root ["alias/dev" "src"]
             f (file-seq (io/file root))
             :let [f-str (str f)]
             :when (and (.isDirectory f)
                        (not (str/includes? f-str "/cljs/"))
                        (not (str/ends-with? f-str "/cljs")))]
         f))

;; Integrant configuration.  Use (go) and (reset) to start and reload, respectively.
#_(def system-cfg {:relembra.config/secrets {}
                 :relembra.crux/node {}
                 :relembra.ring-handler/handler {:crux-node (ig/ref :relembra.crux/node)
                                                 :secrets (ig/ref :relembra.config/secrets)}
                 :relembra.jetty/server {:handler (ig/ref :relembra.ring-handler/handler)}})

(def system-cfg {:relembra.crux/node {}
                 :relembra.ring-handler/handler {:crux-node (ig/ref :relembra.crux/node)}
                 :relembra.jetty/server {:handler (ig/ref :relembra.ring-handler/handler)}})
(ig/load-namespaces system-cfg)
(integrant.repl/set-prep! (constantly system-cfg))


(defn hotload-dep
  "Only works with -A:hotload-deps"
  [& args]
  (if-let [f
           (try (requiring-resolve 'clojure.tools.deps.alpha.repl/add-lib)
                (catch java.io.FileNotFoundException _ nil))]
    (apply f args)
    (debug "dep hotloading is disabled; enable with -A:hotload-deps")))


(comment
  ;; start system like this
  (go)
  ;; soft restart system like this
  (reset)
  ;; hard stop system like this
  (halt)

  (require '[relembra.ring-handler :as rh])

  )
