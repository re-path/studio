(ns repath.studio.tools.image
  (:require [repath.studio.elements.handlers :as elements]
            [repath.studio.tools.base :as tools]))

(derive :image ::tools/graphics)

(defmethod tools/properties :image [] {:icon "image"
                                       :description "The <image> SVG element includes images inside SVG documents. It can display raster image files or other SVG files."
                                       :attrs [:href]})

(defmethod tools/drag :image
  [{:keys [adjusted-mouse-offset adjusted-mouse-pos] :as db}]
  (let [[offset-x offset-y] adjusted-mouse-offset
        [pos-x pos-y] adjusted-mouse-pos
        attrs {:x      (min pos-x offset-x)
               :y      (min pos-y offset-y)
               :width  (abs (- pos-x offset-x))
               :height (abs (- pos-y offset-y))
               :preserveAspectRatio "xMidYMid slice"}]
    (elements/set-temp db {:type :element :tag :image :attrs attrs})))