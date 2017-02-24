(ns chat42.core
  (:require [hasch.core :refer [uuid]]
            [replikativ.peer :refer [server-peer]]

            [kabel.peer :refer [start stop]]
            [konserve.memory :refer [new-mem-store]]

            [superv.async :refer [<?? S]] ;; core.async error handling
            [clojure.core.async :refer [chan] :as async]))

(def uri "ws://127.0.0.1:31744")

(defn -main [& args]
  (let [store (<?? S (new-mem-store))
        peer (<?? S (server-peer S store uri))]
    (<?? S (start peer))
    (println "Chat42 replikativ server peer up and running!" uri)
    ;; HACK blocking main termination
    (<?? S (chan))))




