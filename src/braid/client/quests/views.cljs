(ns braid.client.quests.views
  (:require [braid.client.state :refer [subscribe]]
            [braid.client.quests.list :refer [quests]]
            [braid.client.dispatcher :refer [dispatch!]]))

(defn quest-view [quest]
  [:div.quest {:data-icon (quest :icon)}
   [:div.info
    [:h1 (quest :name)]
    [:div.progress
     (for [i (range (quest :goal))]
       [:div.icon {:class (if (< i (quest :progress))
                            "complete"
                            "incomplete")}])]
    [:p (or (quest :description) "A short description would be here. Lorem ispum dolor it amet.")]]

   (if (< (quest :progress) (quest :goal))
     [:div.actions
      [:a.skip {:on-click (fn [_]
                            (dispatch! :quests/skip-quest (quest :id)))} "Skip"]
      [:a.show-me {:on-click (fn [_]
                               (dispatch! :quests/show-quest-instructions (quest :id)))}
       "Show Me"]]
     [:div.actions
      [:a.next {:on-click (fn [_]
                            (dispatch! :quests/get-next-quest (quest :id)))}
       "Get Next Quest"]])])

(defn quests-menu-view []
  (let [active-quests (subscribe [:quests/active-quests])]
    (fn []
      [:div.quests-menu
       [:div.content
        [:div.quests
         (for [quest @active-quests]
           ^{:key (quest :id)}
           [quest-view quest])]]])))

(defn quests-header-view []
  (let [completed-quest-count (subscribe [:quests/completed-quest-count])]
    (fn []
      [:div.quests-header
       [:div.bar
        @completed-quest-count
        " "
        "Quests"]
       [quests-menu-view]])))

