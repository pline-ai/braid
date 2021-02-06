(ns braid.core.client.ui.styles.thread
  (:require
   [braid.core.client.ui.styles.header :as headers]
   [braid.core.client.ui.styles.misc :refer [drag-and-drop]]
   [braid.core.client.ui.styles.mixins :as mixins]
   [braid.core.client.ui.styles.vars :as vars]
   [garden.arithmetic :as m]
   [garden.units :refer [px em rem]]))

(defn head [pad]
  [:>.head
   {:min-height "3.5em"
    :position "relative"
    :width "100%"
    :flex-shrink 0
    :padding [[pad (m/* 2 pad) pad pad]]
    :box-sizing "border-box"}

   [:>.tags
    {:display "inline"}

    [:>.remove-mention
     mixins/pill-button
     {:margin-left "-0.25rem"
      :margin-right "0.25rem"}]

    [:.add
     {:position "relative"}

     [:>span.pill
      mixins/pill-button
      {:letter-spacing "normal !important"}]]

    [:>.user :>.tag :>.add
     {:display "inline-block"
      :vertical-align "middle"
      :margin-bottom (rem 0.25)
      :margin-right (rem 0.25)}]]

   [:>.controls
    {:position "absolute"
     :padding pad
     :top 0
     :right 0
     :z-index 10}

    [:>.extras
     {:display "none"}]

    [:&:hover>.extras
     {:display "block"}]

    [:>.main>.control
     :>.extras>.control
     {:cursor "pointer"
      :font-family "fontawesome"
      :text-align "center"
      :display "block"
      :text-decoration "none"
      :color "#CCC"
      :margin-bottom (m/* pad 0.5)
      ; needs to accomodate widest icon
      ; otherwise, x button moves
      :width "1.1em"}

     [:&:hover
      {:color "#333"}]]]])

(defn messages [pad]
  [:>.messages
   {:position "relative"
    :overflow-y "auto"
    :overflow-x "hidden"}

   [:>.divider
    {:text-align "center"
     :height "1em"
     :color "#999"
     :padding "0 0.5rem 0.5rem"
     :position "relative"}

    [:>.border
     (mixins/card-border "9px")]

    [:>.date
     {:display "inline-block"
      :padding "0 0.5em"
      :background "white"
      :letter-spacing "0.02em"
      ;; so that it gets a z-index
      :position "relative"}]

    [:&:before
     {:content "\"\""
      :border-bottom "1px solid #ccc"
      :margin-top "-1px"
      :width "100%"
      :display "block"
      :position "absolute"
      :top "50%"}]]])

(defn thread [pad]
  [:>.thread
   mixins/flex
   {:margin-right pad
    :min-width vars/card-width
    :width vars/card-width
    :box-sizing "border-box"
    :outline "none"
    :flex-direction "column"
    :height "100%"
    :z-index 101}

   [:&.new
    {:z-index 99}]

   ; switch to ::after to align at top
   [:&::before
    {:content "\"\""
     :flex-grow 1
     ; have 1px so card shadow shows
     :min-height "1px"}]

   ;; XXX: does this class actually apply to anything?
   [:&.archived :&.limbo :&.private
    [:>.head::before
     {:content "\"\""
      :display "block"
      :width "100%"
      :height (px 5)
      :position "absolute"
      :top 0
      :left 0
      :border-radius [[vars/border-radius
                       vars/border-radius 0 0]]}]

    [:&.archived
     [:.head::before
      {:background vars/archived-thread-accent-color}]]

    [:&.private
     [:.head::before
      {:background vars/private-thread-accent-color}]]

    [:&.limbo
     [:.head::before
      {:background vars/limbo-thread-accent-color}]]]

   [:&.focused
    [:>.card
     [:>.border
      (mixins/card-border "5px")
      {:border-radius [[vars/border-radius 0 0 0]]}]]]

   [:>.card
    mixins/flex
    {:flex-direction "column"
     :box-shadow [[0 (px 1) (px 4) 0 "#c3c3c3"]]
     :max-height "100%"
     :background "white"
     :border-radius [[vars/border-radius
                      vars/border-radius 0 0]]
     :position "relative"}

    (drag-and-drop pad)
    (head pad)
    (messages pad)

    [:>.join
     (headers/header-button pad)
     {:margin-bottom "0.5em"}]]])

(defn notice [pad]
  [:>.thread
   [:>.notice
    {:box-shadow [[0 (px 1) (px 2) 0 "#ccc"]]
     :padding pad
     :margin-bottom pad}

    [:&::before
     {:float "left"
      :font-size vars/avatar-size
      :margin-right (rem 0.5)
      :content "\"\""}]]

   [:&.private :&.limbo
    [:>.card
     {; needs to be a better way
      ; which is based on the height of the notice
      :max-height "85%"}]]

   [:&.private
    [:>.notice
     {:background "#D2E7FF"
      :color vars/private-thread-accent-color}

     [:&::before
      (mixins/fontawesome \uf21b)]]]

   [:&.limbo
    [:>.notice
     {:background "#ffe4e4"
      :color vars/limbo-thread-accent-color}

     [:&::before
      (mixins/fontawesome \uf071)]]]])


(defn new-message [pad]
  [:>.message.new
   {:flex-shrink 0
    :padding pad
    :margin 0
    :position "relative"}

   [:>.plus
    {:border-radius vars/border-radius
     :text-align "center"
     :line-height (em 2)
     :position "absolute"
     :top 0
     :bottom 0
     :left 0
     :width vars/avatar-size
     :margin pad
     :cursor "pointer"
     :color "#e6e6e6"
     :box-shadow "0 0 1px 1px #e6e6e6"}

    [:&::after
     {:position "absolute"
      :top "50%"
      :left 0
      :width "100%"
      :margin-top (em -1)}
      (mixins/fontawesome \uf067)]

    [:&:hover
     {:color "#ccc"
      :box-shadow "0 0 1px 1px #ccc"}]

    [:&:active
     {:color "#999"
      :box-shadow "0 0 1px 1px #999"}]

    [:&.uploading::after
     (mixins/fontawesome \uf110)
     mixins/spin]]

   [:>.autocomplete-wrapper

    [:.textarea

     [:>textarea
      {:width "100%"
       :resize "none"
       :border "none"
       :box-sizing "border-box"
       :min-height (em 3.5)
       :padding-left (rem 2.5)}

      [:&:focus
       {:outline "none"}]]]

    [:>.autocomplete
     {:z-index 1000
      :box-shadow [[0 (px 1) (px 4) 0 "#ccc"]]
      :background "white"
      :max-height (em 20)
      :overflow "auto"
      :width vars/card-width
      ; will be an issue when text area expands:
      :position "absolute"
      :bottom (m/* pad 3)}

     [:>.result
      {:padding "0.25em 0.5em"}

      [:>.match
       {:display "flex"}

       [:>.avatar
        :>.color-block
        {:display "block"
         :height "2em"
         :margin "0.25em 0.5em 0.25em 0"}]

       [:>.avatar
        {:width "2em"}]

       [:>.color-block
        {:width "1em"
         :border "3px solid #fff"
         :border-radius "3px"
         :box-sizing "border-box"}]

       [:>.info
        {:margin "0.25em 0"}

        [:>.name
         {:height "1em"
          :white-space "nowrap"}]

        [:>.extra
         {:color "#ccc"
          :overflow-y "hidden"
          :width "100%"
          :max-height "1em"}]]]

      [:&:hover
       {:background "#eee"}]

      [:&.highlight
       [:.name
        {:font-weight "bold"}]]]]]])


(def add-tag-popover-styles
  [:>.add-mention-popup
   {:position "absolute"
    :background-color "white"
    :margin-left (em 0.5)
    :margin-bottom (em 0.5)
    :background "white"
    :z-index 100
    :width "50vw"
    :height "50vh"
    :overflow-x "hidden"
    :display "flex"
    :flex-direction "column"
    :border-radius "0.25em"}

   [:>.search
    {:font-size "1.2em"}]

   [:>.cancel
    (mixins/settings-button)]

   [:>.search-results
    {:display "flex"
     :flex-direction "row"
     :flex-grow "2"
     :min-height "0px"}

    [:>.tag-list
     :>.user-list
     {:flex-grow "1"
      :overflow-y "scroll"}

     [:>.tag-option
      :>.user-option
      {:cursor "pointer"
       :white-space "nowrap"
       :padding (em 0.25)}

      [:&:hover
       {:background "#eee"}]

      [:&.selected
       {:font-weight "bold"
        :background-color "rgba(0, 0, 0, 0.1)"}]

      [:>.rect
       {:width (em 1)
        :height (em 2)
        :display "inline-block"
        :vertical-align "middle"
        :border-radius (px 3)}]

      [:>span
       {:margin (rem 0.25)
        :display "inline-block"
        :vertical-align "middle"}]]]]])
