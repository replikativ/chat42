(ns chat42.core
  (:require [replikativ.crdt.cdvcs.realize :refer [head-value stream-into-identity!]]
            [replikativ.crdt.cdvcs.stage :as s]
            [replikativ.stage :refer [create-stage! connect! subscribe-crdts!]]
            [replikativ.peer :refer [client-peer server-peer]]

            [kabel.peer :refer [start stop]]
            [konserve.memory :refer [new-mem-store]]
            [konserve.filestore :refer [new-fs-store delete-store]]

            [superv.async :refer [<?? <? S go-try go-loop-try]] ;; core.async error handling
            [clojure.core.async :refer [chan go-loop go] :as async]))

(def user "mail:alice@replikativ.io")
(def cdvcs-id #uuid "7d274663-9396-4247-910b-409ae35fe98d")
(def uri "ws://127.0.0.1:31744")
(def val-atom (atom {:messages []}))
(def store-path "/tmp/chat42-store")

(def eval-fns
  {'(fn [_ new] [new]) (fn [_ new] {:messages [new]})
   'conj (fn [old new] (update old :messages conj new))})

(def stream-eval-fns
  {'(fn [_ new] [new]) (fn [a new] (reset! a {:messages [new]}) a)
   'conj (fn [a new] (swap! a (fn [prev next] (update prev :messages conj next)) new) a)})


(comment
  (def store (<?? S (new-mem-store)))
  
  (def peer (<?? S (server-peer S store uri)))

  (<?? S (start peer))

  ;; to interact with a peer we use a stage
  (def stage (<?? S (create-stage! user peer)))

  ;; create a new CDVCS
  (<?? S (s/create-cdvcs! stage :description "testing" :id cdvcs-id))

  (def close-stream
    (stream-into-identity! stage [user cdvcs-id] stream-eval-fns val-atom))

  (async/close! close-stream)

  @val-atom

  (<?? S (stop server))

  )


