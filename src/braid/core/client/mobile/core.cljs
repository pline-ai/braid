(ns braid.core.client.mobile.core
  (:require
   [braid.base.client.events]
   [braid.base.client.subs]
   [braid.chat.client.events]
   [braid.chat.client.subs]
   [braid.core.client.gateway.events]
   [braid.core.client.gateway.subs]
   [braid.core.client.mobile.auth-flow.events]
   [braid.core.client.mobile.auth-flow.routes]
   [braid.core.client.mobile.auth-flow.subs]
   [braid.core.client.mobile.views :refer [app-view]]
   [braid.base.client.router :as router]
   [braid.core.client.routes]
   [braid.base.client.remote-handlers]
   [braid.core.modules :as modules]
   [re-frame.core :refer [dispatch-sync dispatch]]
   [reagent.dom :as r-dom]))

(enable-console-print!)

(defn render []
  (r-dom/render [app-view] (. js/document (getElementById "app"))))

(defn ^:export init []
  (modules/init! modules/default)
  (dispatch-sync [:initialize-db])
  (render)
  (router/init))

(defn ^:export reload []
  (modules/init! modules/default)
  (render))
