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

(defmethod ig/init-key ::node [_ {:keys [dir]}]
  (let [crux-node (crux/start-node (if dir
                                     {:gold-store {:crux/module 'crux.lmdb/->kv-store
                                                   :db-dir (io/file dir)}
                                      ;; I don't bother with an index store,
                                      ;; since I expect I'll have small DBs
                                      :crux/document-store {:kv-store :gold-store}
                                      :crux/tx-log {:kv-store :gold-store}}
                                     {}))]
    (upsert-functions! crux-node)
    crux-node))


(defmethod ig/halt-key! ::node [_ ^ICruxAPI crux-node]
  (.close crux-node))


(comment

  (do
    (require '[integrant.repl.state :refer (system)])
    (require '[tick.alpha.api :as t])

    ;; the chicago 7

    (def crux-node (:relembra.crux/node system)))

  (def d1 (t/date "2020-08-01"))
  (def d2 (t/date "2020-10-01"))

  (t/< d1 d2)

  (def crux-node (crux/start-node {}))


  (sync-tx crux-node [[:crux.tx/put {:crux.db/id :a :neym "a"}]
                      [:crux.tx/put {:crux.db/id :b :neym "b"}]
                      [:crux.tx/put {:crux.db/id :c :neym "c"}]
                      [:crux.tx/put {:crux.db/id :e :ref :a :t (t/date "2020-10-01")}]
                      [:crux.tx/put {:crux.db/id :f :ref :b :t (t/date "2022-10-01")}]])

  (crux/q (crux/db crux-node)
          {:find ['who]
           :where '[[who :name]
                    (not [who :surname])]})


  (def d (t/date "2020-09-01"))
  (crux/q (crux/db crux-node)
          {:find ['x]
           :where '[[x :neym]
                    (not [_ :ref x])]})

  (crux/q (crux/db crux-node) {:find ['k] :where [['k :crux.db/fn]]})

  (upsert-functions! crux-node)

  transaction-functions

  (alter-var-root #'transaction-functions (constantly {}))

  )
