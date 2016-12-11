(ns chat42.core
  (:require [konserve.memory :refer [new-mem-store]]
            [replikativ.peer :refer [client-peer]]
            [replikativ.stage :refer [create-stage! connect! subscribe-crdts!]]
            [replikativ.crdt.cdvcs.realize :refer [stream-into-identity!]]
            [replikativ.crdt.cdvcs.stage :as s]
            [replikativ.crdt.cdvcs.realize :refer [head-value]]
            [cljs.core.async :refer [>! chan timeout]]
            [superv.async :refer [throw-if-exception S] :as sasync]
            [cljsjs.material-ui] ;; TODO why?
            [om.next :as om :refer-macros [defui] :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs-react-material-ui.core :as ui]
            [cljs-react-material-ui.icons :as ic]
            [sablono.core :as html :refer-macros [html]]
            [cljs-react-material-ui.core :as ui]
            [cljs-react-material-ui.icons :as ic])
  (:require-macros [superv.async :refer [go-try <? go-loop-try]]
                   [cljs.core.async.macros :refer [go-loop]]))

(enable-console-print!)


(def user "mail:alice@replikativ.io")
(def cdvcs-id #uuid "7d274663-9396-4247-910b-409ae35fe98d")
(def uri "ws://127.0.0.1:31744")

(def eval-fns
  {'(fn [_ new] [new]) (fn [_ new] {:messages [new]})
   'conj (fn [old new] (update old :messages conj new))})

(def stream-eval-fns
  {'(fn [_ new] [new]) (fn [a new] (reset! a {:messages [new]}) a)
   'conj (fn [a new] (swap! a (fn [prev next] (update prev :messages conj next)) new) a)})

(defn create-msg [name text]
  {:text text :name name :date (js/Date.)})

(def val-atom (atom {:messages [(create-msg "Greetings" "Chat42")]}))


(defn start-local []
  (go-try S
   (let [local-store (<? S (new-mem-store))
         local-peer (<? S (client-peer S local-store))
         stage (<? S (create-stage! user local-peer))]
     {:store local-store
      :stage stage
      :peer local-peer})))

(defn init []
  (go-try S
   (def client-state (<? S (start-local)))

   (stream-into-identity! (:stage client-state)
                          [user cdvcs-id]
                          stream-eval-fns
                          val-atom)
   (<? S (s/create-cdvcs! (:stage client-state) :description "testing" :id cdvcs-id))
   (<? S (connect! (:stage client-state) uri))))

(defn send-message! [app-state msg]
  (go-try S (<? S (s/transact! (:stage client-state) [user cdvcs-id] [['conj msg]]))))

(defn target-val [e]
  (.. e -target -value))

(defui App
  Object
  (componentDidMount [this]
                     (.log js/console "will mount")
                     (om/set-state! this {:input-name "Chat42" :input-text "Hi"}))
  (render [this]
    (let [app-state (om/props this)
          {:keys [input-name input-text]} (om/get-state this)]
      (html
       [:div
        [:div
         [:input {:value input-name
                            :on-change
                            (fn [e]
                              (om/update-state! this assoc :input-name (target-val e)))}]
         [:input {:value input-text
                          :on-change (fn [e]
                                       (om/update-state! this assoc :input-text (target-val e)))}]
         [:button {:on-click (fn [_] (send-message! app-state (create-msg input-name input-text)))} "Send"]]
        (map (fn [{:keys [text name date]}]
               [:p (str date ": " name " -> " text)])
             (:messages app-state))]))))

(def reconciler
  (om/reconciler {:state val-atom}))

(defn main [& args]
  (init)
  (om/add-root! reconciler App (.getElementById js/document "app"))
  (set! (.-onclick (.getElementById js/document "send")) send-message!))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  )
