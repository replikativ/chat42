(ns chat42.core
  (:gen-class :main true)
  (:require [hasch.core :refer [uuid]]
            [replikativ.crdt.ormap.realize :refer [stream-into-identity!]]
            [replikativ.crdt.ormap.stage :as s]
            [replikativ.stage :refer [create-stage! connect! subscribe-crdts!]]
            [replikativ.peer :refer [client-peer server-peer]]

            [kabel.peer :refer [start stop]]
            [konserve.memory :refer [new-mem-store]]
            [konserve.filestore :refer [new-fs-store delete-store]]

            [superv.async :refer [<?? <? S go-try go-loop-try]] ;; core.async error handling
            [clojure.core.async :refer [chan go-loop go] :as async]))

(def user "mail:alice@replikativ.io")
(def ormap-id #uuid "7d274663-9396-4247-910b-409ae35fe98d")
(def uri "ws://127.0.0.1:31744")
(def val-atom (atom {}))
(def store-path "/tmp/chat42-store")
(defonce server-state (atom nil))

(def stream-eval-fns
  {'(fn [_ new] [new]) (fn [a new]
                        (reset! a {(uuid new) new})
                        a)
   'assoc (fn [a new]
            (swap! a assoc (uuid new) new)
            a)
   'dissoc (fn [a new]
             (swap! a dissoc (uuid new))
             a)})

(defn start-all-services []
  (let [store (<?? S (new-mem-store))
        peer (<?? S (server-peer S store uri))
        _ (<?? S (start peer))
        stage (<?? S (create-stage! user peer))
        close-stream (stream-into-identity! stage [user ormap-id] stream-eval-fns val-atom)]
    ;; just to print messages to STDOUT
    (add-watch val-atom :messages
               (fn [_ _ _ val]
                 (let [{:keys [date name text]} (-> val vals last)]
                   (println (str "Date: " (.toString (java.util.Date. date)) " name: " name " text: " text)))))
    (<?? S (s/create-ormap! stage :description "messages" :id ormap-id))
    (println "Chat42 replikativ server peer up and running!")
    
    {:store store
     :peer peer
     :close-stream close-stream
     :stage stage}))

(defn -main [& args]
  (let [c (chan)]
    (reset! server-state (start-all-services))
    (<?? S c)))

(comment

  (def state (start-all-services))
  
  @val-atom
  
  (async/close! (:close-stream state))

  (<?? S (stop (:peer state)))

  (reset! val-atom nil)

  (def test-msg {:text "Hi" :name "Yo" :date (.getTime (java.util.Date.))})

  (<?? S (s/assoc! (:stage state) [user ormap-id] (uuid test-msg) [['assoc test-msg]]))
 

  )


