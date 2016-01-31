(ns chat.client.views.groups-nav
  (:require [om.core :as om]
            [om.dom :as dom]
            [clojure.string :as string]
            [chat.client.store :as store]
            [chat.client.views.helpers :refer [id->color]]))

(defn groups-nav-view [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "groups-nav"}

        (apply dom/div #js {:className "groups"}
          (map (fn [group]
                 (dom/div #js {:className (str "group "
                                               (when (= (@store/app-state :open-group-id)  (group :id)) "active"))
                               :style #js {:backgroundColor (id->color (group :id))}
                               :title (group :name)
                               :onClick (fn [e]
                                          (store/set-open-group! (group :id))
                                          (store/set-page! {:type :inbox}))}
                   (string/join "" (take 2 (group :name)))
                   (let [cnt (->>
                               (select-keys (data :threads) (get-in data [:user :open-thread-ids]))
                               vals
                               (filter (fn [thread]
                                         (contains? (set (->> (thread :tag-ids)
                                                              (map  (fn [tag-id]
                                                                      (get-in @store/app-state [:tags tag-id :group-id]))))) (group :id))))
                               count)]
                     (when (< 0 cnt)
                       (dom/span #js {:className "count"} cnt)))))
               (vals (data :groups))))

        (dom/div #js {:className "plus"
                      :onClick (fn [_] (store/set-page! {:type :group-explore}))} "")))))
