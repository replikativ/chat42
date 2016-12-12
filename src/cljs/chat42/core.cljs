(ns chat42.core
  (:require [konserve.memory :refer [new-mem-store]]
            [replikativ.peer :refer [client-peer]]
            [replikativ.stage :refer [create-stage! connect! subscribe-crdts!]]

            [hasch.core :refer [uuid]]
            
            [replikativ.crdt.ormap.realize :refer [stream-into-identity!]]
            [replikativ.crdt.ormap.stage :as s]
            
            [cljs.core.async :refer [>! chan timeout]]
            [superv.async :refer [S] :as sasync]
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
(def ormap-id #uuid "7d274663-9396-4247-910b-409ae35fe98d")
(def uri "ws://127.0.0.1:31744")

(def stream-eval-fns
  {'(fn [_ new] [new]) (fn [a new] (reset! a {(uuid new) new}) a)
   'assoc (fn [a new] (swap! a assoc (uuid new) new))
   'dissoc (fn [a new] (swap! a dissoc (uuid new)))})

(defn create-msg [name text]
  {:text text :name name :date (.getTime (js/Date.))})

(defonce val-atom (atom {}))

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
                          [user ormap-id]
                          stream-eval-fns
                          val-atom)
   #_(<? S (s/create-ormap! (:stage client-state) :description "testing" :id ormap-id))
   (<? S (s/create-ormap! (:stage client-state) :description "messages" :id ormap-id))
   (<? S (connect! (:stage client-state) uri))))

(defn send-message! [app-state msg]
  (go-try S (<? S (s/assoc! (:stage client-state) [user ormap-id] (uuid msg) [['assoc msg]]))))

(defn target-val [e]
  (.. e -target -value))

(defui App
  Object
  (componentDidMount [this]
                     (.log js/console "will mount")
                     (om/set-state! this {:input-name "" :input-text ""}))
  (render [this]
    (let [app-state (om/props this)
          {:keys [input-name input-text]} (om/get-state this)]
      (html
       [:div
        [:div
         [:input {:value input-name
                  :placeholder "Name"
                  :on-change
                  (fn [e]
                    (om/update-state! this assoc :input-name (target-val e)))}]
         [:input {:value input-text
                  :placeholder "Message"
                  :on-change (fn [e]
                               (om/update-state! this assoc :input-text (target-val e)))}]
         [:button {:on-click (fn [_]
                               (do
                                 (send-message! app-state (create-msg input-name input-text))
                                 (om/update-state! this assoc :input-text "")))} "Send"]]
        (map (fn [{:keys [text name date]}]
               [:p (str (.toDateString (js/Date. date)) ": " name " -> " text)])
             (vals app-state))]))))

(def reconciler
  (om/reconciler {:state val-atom}))

(defn main [& args]
  (init)
  (set! (.-onclick (.getElementById js/document "send")) send-message!))

;; for figwheel not in main
(om/add-root! reconciler App (.getElementById js/document "app"))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  )
