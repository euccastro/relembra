(ns relembra.crux
  (:require [crux.api :as crux]
            [clojure.java.io :as io]
            [integrant.core :as ig])
  (:import (crux.api ICruxAPI)))


(defonce ^:private transaction-functions {})


(defn add-transaction-function! [k body]
  (alter-var-root #'transaction-functions assoc k body))


(defn- upsert-functions! [crux-node]
  (some->> (seq (for [[k body] transaction-functions
                      :let [old-body (:crux.db/fn (crux/entity (crux/db crux-node) k))]
                      :when (not= old-body body)]
                  [:crux.tx/put {:crux.db/id k :crux.db/fn body}]))
    (crux/submit-tx crux-node)))


(defn q [crux-node query]
  (crux/q (crux/db crux-node) query))


(defn q1 [crux-node query]
  (ffirst (q crux-node query)))


(defn sync-tx [crux-node tx-data]
  (let [tx (crux/submit-tx crux-node tx-data)]
    (crux/sync crux-node)
    (crux/tx-committed? crux-node tx)))


(defn update-entity [crux-node eid f & args]
  (let [e (crux/entity (crux/db crux-node) eid)]
    (sync-tx crux-node
             [[:crux.tx/match eid e]
              [:crux.tx/put (apply f e args)]])))


(defmethod ig/init-key ::node [_ {:keys [dir init-fn]}]
  (let [crux-node
        (crux/start-node
         (cond->
             {:crux.node/topology
              (cond-> '[crux.standalone/topology]
                dir (conj 'crux.kv.lmdb/kv-store) )}
             dir (assoc :crux.kv/db-dir (str (io/file dir "db"))
                        :crux.standalone/event-log-kv-store 'crux.kv.lmdb/kv
                        :crux.standalone/event-log-dir (str (io/file dir "event-log")))))]
    (when init-fn (init-fn crux-node))
    (upsert-functions! crux-node)
    crux-node))


(defmethod ig/halt-key! ::node [_ ^ICruxAPI crux-node]
  (.close crux-node))


(comment

  (require '[integrant.repl.state :refer (system)])

  (def crux-node (:relembra.crux/node system))

  (crux/q (crux/db crux-node) {:find ['k] :where [['k :crux.db/fn]]})

  (upsert-functions! crux-node)

  transaction-functions

  (alter-var-root #'transaction-functions (constantly {}))

  )
