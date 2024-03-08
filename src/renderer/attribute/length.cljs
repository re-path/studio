(ns renderer.attribute.length
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Content_type#length"
  (:require
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as hierarchy]
   [renderer.attribute.views :as v]
   [renderer.components :as comp]
   [renderer.utils.units :as units]))

(derive :x ::length)
(derive :y ::length)
(derive :x1 ::length)
(derive :y1 ::length)
(derive :x2 ::length)
(derive :y2 ::length)
(derive :cx ::length)
(derive :cy ::length)
(derive :dx ::length)
(derive :dy ::length)

(derive ::positive-length ::length)

(derive :width ::positive-length)
(derive :height ::positive-length)
(derive :stroke-width ::positive-length)
(derive :r ::positive-length)
(derive :rx ::positive-length)
(derive :ry ::positive-length)

(defmethod hierarchy/form-element ::length
  [k v disabled? initial]
  [:div.flex.w-full
   [v/form-input
    {:key k
     :value v
     :disabled? disabled?
     :placeholder (if v initial "multiple")
     :on-wheel (fn [e]
                 (if (pos? (.-deltaY e))
                   (rf/dispatch [:element/update-attr k - 1])
                   (rf/dispatch [:element/update-attr k + 1])))}]
   [:div.flex {:style {:width "54px"}}
    [:button.button.ml-px.bg-primary.text-muted
     {:style {:width "26px" :height "26px"}
      :on-pointer-down #(rf/dispatch [:element/update-attr k - 1])}
     [comp/icon "minus" {:class "icon small"}]]
    [:button.button..ml-px.bg-primary.text-muted
     {:style {:width "26px" :height "26px"}
      :on-click #(rf/dispatch [:element/update-attr k + 1])}
     [comp/icon "plus" {:class "icon small"}]]]])

(defmethod hierarchy/update-attr ::length
  [element attribute f & more]
  (update-in element [:attrs attribute] #(apply units/transform % f more)))

(defmethod hierarchy/update-attr ::positive-length
  [element attribute f & more]
  (update-in element [:attrs attribute]
             #(units/transform % (fn [v] (max 0 (apply f v more))))))
