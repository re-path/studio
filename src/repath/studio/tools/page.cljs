(ns repath.studio.tools.page
  (:require [re-frame.core :as rf]
            [repath.studio.tools.base :as tools]
            [repath.studio.elements.handlers :as elements]
            [repath.studio.mouse :as mouse]
            [clojure.core.matrix :as matrix]
            [repath.studio.units :as units]
            [clojure.string :as str]
            [reagent.dom.server :as dom]
            [goog.string :as gstring]))

(derive :page ::tools/container)

(defmethod tools/properties :page [] {:icon "page"
                                      :description "The page is a top level SVG element with some extra custom attributes."
                                      :attrs [:overflow]})

(defmethod tools/drag :page
  [{:keys [adjusted-mouse-pos adjusted-mouse-offset] :as db}]
  (let [[offset-x offset-y] adjusted-mouse-offset
        [pos-x pos-y] adjusted-mouse-pos
        attrs {:x      (min pos-x offset-x)
               :y      (min pos-y offset-y)
               :width  (abs (- pos-x offset-x))
               :height (abs (- pos-y offset-y))
               :fill   "#ffffff"}]
    (elements/set-temp db {:tag :page
                           :type :element
                           :name "Page"
                           :attrs attrs})))

(defmethod tools/bounds :page
  [{:keys [attrs]}]
  (let [{:keys [x y width height stroke-width stroke]} attrs
        [x y width height stroke-width-px] (mapv units/unit->px [x y width height stroke-width])
        stroke-width-px (if (str/blank? stroke-width) 1 stroke-width-px)
        [x y] (matrix/sub [x y] (/ (if (str/blank? stroke) 0 stroke-width-px) 2))
        [width height] (matrix/add [width height] (if (str/blank? stroke) 0 stroke-width-px))]
    (mapv units/unit->px [x y (+ x width) (+ y height)])))

(defmethod tools/render :page
  [{:keys [attrs children key type] :as element}]
  (let [child-elements @(rf/subscribe [:elements/filter-visible children])
        rect-attrs     (select-keys attrs [:x :y :width :height])
        text-attrs     (select-keys attrs [:x :y])
        filter         @(rf/subscribe [:filter])
        zoom           @(rf/subscribe [:zoom])]
    [:g {:key key}
     [:text (merge (update text-attrs :y - (/ 10 zoom)) {:on-mouse-up     #(mouse/event-handler % element)
                                                         :on-mouse-down   #(mouse/event-handler % element)
                                                         :on-mouse-move   #(mouse/event-handler % element)
                                                         :fill "#888"
                                                         :font-size (/ 12 zoom)
                                                         :font-family "Source Code Pro, monospace"}) (or (:name element) type)]

     [:rect (merge rect-attrs {:fill "rgba(0, 0, 0, .1)"
                               :transform (str "translate(" (/ 2 zoom) " " (/ 2 zoom) ")")
                               :style {:filter (str "blur(" (/ 2 zoom) "px)")}})]
     [:svg  (cond-> attrs
              :always (dissoc :fill)
              (not= filter :no-filter) (assoc :filter (str "url(#" (name filter) ")")))
      (when (:fill attrs) [:rect (merge rect-attrs {:x 0
                                                    :y 0
                                                    :fill (:fill attrs)
                                                    :on-mouse-up   #(mouse/event-handler % element)
                                                    :on-double-click #(mouse/event-handler % element)})])
      (map (fn [element] ^{:key (:key element)} [tools/render element]) (merge child-elements))]]))

(defmethod tools/area :page [])

(defmethod tools/render-to-string :page
  [{:keys [attrs children]}]
  (let [child-elements @(rf/subscribe [:elements/filter-visible children])]
    (gstring/unescapeEntities (dom/render-to-static-markup [:svg (dissoc attrs :fill) (map tools/render-to-string (merge child-elements))]))))