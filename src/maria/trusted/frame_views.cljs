(ns maria.trusted.frame-views
  (:require [re-view.core :as v :refer [defview]]
            [re-db.d :as d]
            [maria.frame-communication :as frame]
            [maria.persistence.local :as local]
            [cljs.core.match :refer-macros [match]]
            [maria.trusted.trusted-actions :as actions]))

(defview frame-view
  {:life/initial-state      #(do {:frame-id (str (gensym))})
   :spec/props              {:id         {:spec :String
                                          :doc  "unique ID for frame"}
                             :on-message {:spec :Function
                                          :doc  "Function to be called with messages from iFrame."}
                             #_:on-change  #_{:spec :Function
                                              :doc  "Function to be called with source, whenever it changes."}
                             #_:on-save    #_{:spec :Function
                                              :doc  "Function to be called with source, whenever 'save' command is fired"}}
   :send                    (fn [{:keys [view/state]} message]
                              (frame/send (:frame-id @state) message))
   :send-transactions       (fn [{:keys [db/transactions view/state]}]
                              (frame/send (:frame-id @state) [:db/transactions transactions]))
   :life/did-mount          (fn [{:keys [view/state on-message] :as this}]
                              (when on-message
                                (frame/listen (:frame-id @state) on-message))
                              (.sendTransactions this))
   :life/will-receive-props (fn [{:keys [on-message view/state db/transactions] {prev-tx         :db/transactions
                                                                                 prev-on-message :on-message} :view/prev-props :as this}]
                              (when-not (= on-message prev-on-message)
                                (frame/unlisten (:frame-id @state) prev-on-message)
                                (frame/listen (:frame-id @state) on-message))
                              (when (not= transactions prev-tx)
                                (.sendTransactions this)))
   :life/will-unmount       (fn [{:keys [view/state on-message]}]
                              (frame/unlisten (:frame-id @state) on-message))}
  [{:keys [view/state]}]
  [:iframe.maria-editor-frame
   {:src (str frame/child-origin "/user.html#frame_" (:frame-id @state))}])

(defview editor-frame-view
  {:spec/props              {:default-value :String}
   :life/will-mount         (fn [{:keys [entity-id]}]
                              (local/init-storage entity-id))
   :life/will-receive-props (fn [{entity-id :entity-id {prev-entity-id :entity-id} :view/prev-props}]
                              (when-not (= entity-id prev-entity-id)
                                (local/init-storage entity-id)))}
  [{:keys [entity-id db/transactions]}]
  (frame-view {:db/transactions (into [(or (d/entity :auth-public)
                                           [:db/retract-entity :auth-public])
                                       (some-> entity-id (d/entity))]
                                      transactions)
               :on-message      (actions/editor-message-handler entity-id)}))