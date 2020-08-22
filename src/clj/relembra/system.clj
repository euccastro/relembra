(ns relembra.system
  (:require clojure.pprint
            [clojure.string :as str]
            [integrant.core :as ig]
            integrant.repl
            [hashp.core]                         ; to enable #p data readers
            [clojure.java.io :as io]
            [clojure.tools.namespace.repl :as nsrepl]
            [taoensso.timbre :as timbre :refer [warn]]
            [taoensso.timbre.tools.logging :refer [use-timbre]]))


(defn init-logging [level]
  (add-tap (bound-fn* clojure.pprint/pprint))
  (use-timbre)
  (timbre/swap-config!
   assoc :ns-log-level
   [["relembra.*" level]
    ["*" :info]]))


;; integrant.repl/reset will cause havoc with hot-reloading and figwheel-main
;; unless we restrict these.
(defn restrict-refresh-dirs [roots]
  (apply nsrepl/set-refresh-dirs
         (for [root roots
               f (file-seq (io/file root))
               :let [f-str (str f)]
               :when (and (.isDirectory f)
                          (not (str/includes? f-str "/cljs/"))
                          (not (str/ends-with? f-str "/cljs")))]
           f)))


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


(defn prep [{:keys [db-dir db-init-fn http-port]}]
  (let [system-cfg {:relembra.crux/node {:dir db-dir
                                         :init-fn db-init-fn}
                    :relembra.ring-handler/handler
                    {:crux-node (ig/ref :relembra.crux/node)}
                    :relembra.jetty/server
                    {:port http-port
                     :handler (ig/ref :relembra.ring-handler/handler)}}]
    (ig/load-namespaces system-cfg)
    (integrant.repl/set-prep! (constantly system-cfg))))


(comment



  )
