(ns repath.studio.elements.subs
  (:require
   [re-frame.core :as rf]
   [repath.studio.tools.base :as tools]
   [repath.studio.helpers :as helpers]
   [clojure.core.matrix :as matrix]
   [clojure.set :as set]
   [goog.color :as color]
   ["js-beautify" :as js-beautify]))

(rf/reg-sub
 :elements/element
 :<- [:elements]
 (fn [elements [_ key]] (get elements key)))

(rf/reg-sub
 :elements/canvas
 :<- [:elements]
 (fn [elements _]
   (:canvas elements)))

(rf/reg-sub
 :elements/pages
 :<- [:elements]
 :<- [:elements/canvas]
 (fn [[elements canvas] _]
   (mapv elements (:children canvas))))

(rf/reg-sub
 :elements/active-page
 :<- [:active-page]
 :<- [:elements]
 (fn [[active-page elements] _]
   (get elements active-page)))

(rf/reg-sub
 :elements/xml
  :<- [:elements/active-page]
 (fn [active-page _]
   (js-beautify/html (tools/render-to-string active-page) #js {:indent_size 2})))

(rf/reg-sub
 :elements/filter
 :<- [:elements]
 (fn [elements [_ keys]] (mapv (fn [key] (get elements key)) keys)))

(rf/reg-sub
 :elements/filter-visible
 :<- [:elements]
 (fn [elements [_ keys]] (filter :visible? (mapv (fn [key] (get elements key)) keys))))

(rf/reg-sub
 :elements/selected
 :<- [:elements]
 (fn [elements _]
   (filter :selected? (vals elements))))

(rf/reg-sub
 :elements/selected-keys
  :<- [:elements/selected]
 (fn [selected-elements _]
   (reduce #(conj %1 (:key %2)) #{} selected-elements)))

(rf/reg-sub
 :elements/selected?
 :<- [:elements/selected]
 (fn [selected-elements _]
   (seq selected-elements)))

(rf/reg-sub
 :elements/multiple-selected?
 :<- [:elements/selected]
 (fn [selected-elements _]
   (seq (rest selected-elements))))

(rf/reg-sub
 :elements/group-selected?
 :<- [:elements/selected]
 (fn [selected-elements _]
   (seq (filter #(= (:tag %) :g) selected-elements))))

(rf/reg-sub
 :elements/selected-attrs
 :<- [:elements/selected]
 (fn [selected-elements _]
   (reduce #(helpers/merge-common (fn [v1 v2] (if (= v1 v2) v1 nil)) %1 (tools/attributes %2)) (tools/attributes (first selected-elements)) (rest selected-elements))))

(rf/reg-sub
 :elements/bounds
 :<- [:elements]
 :<- [:elements/selected]
 (fn [[elements selected-elements] _]
   (tools/elements-bounds elements selected-elements)))

(rf/reg-sub
 :elements/area
 :<- [:elements/selected]
 (fn [selected-elements _]
   (reduce (fn [area element] (+ (tools/area element) area)) 0 selected-elements)))

(rf/reg-sub
 :elements/visible
 :<- [:elements]
 (fn [elements _] (filter :visible? (vals elements))))

(rf/reg-sub
 :elements/hovered-or-selected
 :<- [:elements]
 :<- [:hovered-keys]
 :<- [:elements/selected-keys]
 (fn [[elements hovered-keys selected-keys] _]
   (vals (select-keys elements (set/union hovered-keys selected-keys)))))

(rf/reg-sub
 :elements/colors
 :<- [:elements/visible]
 (fn [visible-elements _]
   (reduce (fn [colors element] (let [color (get-in element [:attrs :fill])]
                                  (if (and color (color/isValidColor color))
                                    (conj colors (keyword (:hex (color/parse color))))
                                    colors))) #{} visible-elements)))

(rf/reg-sub
 :snaping-points
 :<- [:elements]
 :<- [:elements/visible]
 (fn [elements visible-elements _]
   (reduce (fn [points element]
             (let [[x1 y1 x2 y2] (tools/adjusted-bounds element elements)
                   [width height] (matrix/sub [x2 y2] [x1 y1])]
               (concat points [[x1 y1]
                               [x1 y2]
                               [x1 (+ y1 (/ height 2))]
                               [x2 y1]
                               [(+ x1 (/ width 2)) y1]
                               [x2 y2]
                               [(+ x1 (/ width 2)) (+ y1 (/ height 2))]]))) [] visible-elements)))
