(ns chat42.core
  (:require [superv.async :refer [<?? S]]
            [kabel.peer :refer [start stop] :as kabel]
            [konserve
             [filestore :refer [new-fs-store]]
             [memory :refer [new-mem-store]]]
            [replikativ
             [peer :refer [server-peer]]
             [stage :refer [connect! create-stage!]]]
            [replikativ.crdt.simple-gset.stage :as gs]
            [replikativ.crdt.ormap.realize :refer [stream-into-identity!]]
            [replikativ.crdt.ormap.stage :as ors]))


(defn msg [txt n]
  {:text txt :name n :date (java.util.Date.)})

(comment
  (def user "mail:prototype@your-domain.com")
  (def gset-id #uuid "7d274663-9396-4247-910b-409ae35fe98d")

  (do
    (def store-a (<?? S (new-mem-store)))
    (def peer-a (<?? S (server-peer S store-a "ws://127.0.0.1:9090"))) ;; network and file IO
    (<?? S (start peer-a))
    (def stage-a (<?? S (create-stage! user peer-a)))
    (<?? S (gs/create-simple-gset! stage-a :id gset-id)))


  (<?? S (gs/add! stage-b [user gset-id] (msg "hi" "alice")))
  
  (<?? S (gs/add! stage-b [user gset-id] (msg "hello" "bob")))

  (get-in @stage-b [user gset-id :state :elements])
  

  (<?? S (connect! stage-a "ws://127.0.0.1:9091"))
  
  (<?? S (gs/add! stage-a [user gset-id] (msg "aloha" "charlie")))

  (get-in @stage-a [user gset-id :state :elements])
  

  (<?? S (stop peer-a))
  
  (<?? S (stop peer-b))

  )

(comment

  (def val-atom (atom {}))
  
  (def stream-eval-fns
    {'(fn [_ new] new) (fn [a new] (reset! a new) a)
   'assoc (fn [a new] (swap! a assoc :foo new) a)})

  
  (def user "mail:prototype@your-domain.com") ;; will be used to authenticate you (not yet)
  
(def ormap-id #uuid "7d274663-9396-4247-910b-409ae35fe98d") ;; application specific datatype address

(def store-a (<?? S (new-fs-store "/tmp/test"))) ;; durable store
(def peer-a (<?? S (server-peer S store-a "ws://127.0.0.1:9090"))) ;; network and file IO
(<?? S (start peer-a))
(def stage-a (<?? S (create-stage! user peer-a))) ;; API for peer
(<?? S (ors/create-ormap! stage-a :id ormap-id))

(def store-b (<?? S (new-mem-store))) ;; store for testing
(def peer-b (<?? S (server-peer S store-b "ws://127.0.0.1:9091")))
(<?? S (start peer-b))
(def stage-b (<?? S (create-stage! user peer-b)))
(<?? S (ors/create-ormap! stage-b :id ormap-id))

(<?? S (stream-into-identity! stage-b [user ormap-id] stream-eval-fns val-atom))

;; now you are set up

;; for this datatype metadata and commit data is separated
;; [['assoc :bars]] is encoding a user-defined function application 
;; of 'store to apply to some local state
(<?? S (ors/assoc! stage-b [user ormap-id] :foo [['assoc :bars]]))
(<?? S (ors/get stage-b [user ormap-id] :foo))

(ffirst (get-in @stage-b [user ormap-id :state :adds :foo]))

(<?? S (connect! stage-a "ws://127.0.0.1:9091")) ;; wire the peers up

(<?? S (ors/get stage-a [user ormap-id] :foo)) ;; do they converge?
;; accordingly we provide a dissoc operation on removal
(<?? S (ors/dissoc! stage-a [user ormap-id] :foo [['dissoc :bars]])) 
;; play around :)

;; ...

(<?? S (stop peer-a))
(<?? S (stop peer-b))

  )
