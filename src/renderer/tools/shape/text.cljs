(ns renderer.tools.shape.text
  (:require
   [clojure.string :as str]
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as hierarchy]
   [renderer.element.handlers :as element.h]
   [renderer.handlers :as h]
   [renderer.history.handlers :as history.h]
   [renderer.tools.base :as tools]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.dom :as dom]
   [renderer.utils.units :as units]))

(derive :text ::tools/renderable)

(defmethod tools/properties :text
  []
  {:icon "text"
   :description "The SVG <text> element draws a graphics element consisting 
                 of text. It's possible to apply a gradient, pattern, 
                 clipping path, mask, or filter to <text>, like any other SVG 
                 graphics element."
   :attrs [:font-family
           :font-size
           :font-weight
           :font-style
           :fill
           :stroke
           :stroke-width
           :opacity]})

(defmethod tools/activate :text
  [db]
  (-> db
      (assoc :cursor "text")
      (h/set-message
       [:div
        [:div "Click to enter your text."]])))

(defmethod tools/pointer-up :text
  [{:keys [adjusted-pointer-offset] :as db}]
  (let [[offset-x offset-y] adjusted-pointer-offset
        attrs {:x offset-x
               :y offset-y}]
    (-> db
        element.h/deselect
        (element.h/create {:type :element
                           :tag :text
                           :attrs attrs})
        (history.h/finalize "Create text")
        (tools/set-tool :edit)
        (h/set-state :edit)))) ; FIXME: Merge create and edit history action.

(defmethod tools/drag-end :text
  [db]
  (tools/pointer-up db))

(defmethod tools/translate :text
  [element [x y]] (-> element
                      (hierarchy/update-attr :x + x)
                      (hierarchy/update-attr :y + y)))

(defn get-text
  [e]
  (str/replace (.. e -target -value) " " "\u00a0")) ; REVIEW

(defn set-text-and-select-element
  [e el-k]
  (.focus (dom/canvas-element)) ; REVIEW
  (rf/dispatch [:element/set-prop
                el-k
                :content
                (get-text e)])
  (rf/dispatch [:set-tool :select]))

(defmethod tools/render-edit :text
  [{:keys [attrs key content] :as el}]
  (let [el-bounds @(rf/subscribe [:element/el-bounds el])
        [x y] el-bounds
        [width height] (bounds/->dimensions el-bounds)
        {:keys [fill font-family font-size font-weight]} attrs]
    [:foreignObject {:x x
                     :y y
                     :width (+ width 20)
                     :height height}
     [:input
      {:key key
       :default-value content
       :auto-focus true
       :on-focus #(.. % -target select)
       :on-pointer-down #(.stopPropagation %)
       :on-pointer-up #(.stopPropagation %)
       :on-blur #(set-text-and-select-element % key)
       :on-key-down (fn [e]
                      (.stopPropagation e)
                      (if (contains? #{"Enter" "Escape"} (.-code e))
                        (set-text-and-select-element e key)
                        (.requestAnimationFrame
                         js/window
                         #(rf/dispatch-sync [:element/preview-prop
                                             key
                                             :content
                                             (get-text e)]))))
       :style {:color "transparent"
               :caret-color (or fill "black")
               :display "block"
               :width (+ width 15)
               :height height
               :padding 0
               :border 0
               :outline "none"
               :background "transparent"
               :font-family (if (empty? font-family) "inherit" font-family)
               :font-size (if (empty? font-size)
                            "inherit"
                            (str (units/unit->px font-size) "px"))
               :font-weight (if (empty? font-weight) "inherit" font-weight)}}]]))

(defmethod tools/path :text
  [{:keys [attrs content]}]
  (.textToPath js/window.api
               content
               #js {:font-url (.-path (first (.findFonts
                                              js/window.api
                                              ;; TODO: Getting the computed styles might safer.
                                              #js {:family (:font-family attrs)
                                                   :weight (js/parseInt (:font-weight attrs))
                                                   :italic (= (:font-style attrs)
                                                              "italic")})))
                    :x (js/parseFloat (:x attrs))
                    :y (js/parseFloat (:y attrs))
                    :font-size (js/parseFloat (or (:font-size attrs) 16))})) ; FIXME
