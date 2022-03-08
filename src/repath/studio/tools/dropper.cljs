(ns repath.studio.tools.dropper
  (:require [repath.studio.tools.base :as tools]
            [clojure.core.matrix :as matrix]
            [repath.studio.styles :as styles]
            [repath.studio.documents.handlers :as documents]
            [repath.studio.history.handlers :as history]
            [goog.color :as gcolor]))

(derive :dropper ::tools/edit)

(defmethod tools/properties :dropper [] {:icon "eye-dropper"})

(defmethod tools/activate :dropper
  [db]
  (js/window.api.send "toMain" #js {:action "beginFrameSubscription"})
  (assoc db :cursor "crosshair"))

(defmethod tools/deactivate :dropper
  [db]
  (js/window.api.send "toMain" #js {:action "endFrameSubscription"})
  (dissoc db :overlay))

(defn get-pixel-color
  [bitmap bitmap-width x y]
  (let [b-index (+ (* y (* 4 bitmap-width))
                   (* x 4))
        b (aget bitmap b-index)
        g (aget bitmap (+ b-index 1))
        r (aget bitmap (+ b-index 2))
        a (aget bitmap (+ b-index 3))]
    [r g b a]))

(defmethod tools/click :dropper
  [{:keys [mouse-pos content-rect] :as db}]
  (let [bitmap (:window/bitmap db)
        size (:window/size db)
        [mouse-x mouse-y] (matrix/add mouse-pos [(:x content-rect) (:y content-rect)])
        color (get-pixel-color bitmap (:width size) mouse-x mouse-y)]
    (-> db
        (documents/set-fill color)
        (history/finalize (str "Pick color " (tools/rgba color))))))

(defmethod tools/mouse-move :dropper
  [{:keys [mouse-pos content-rect] :as db} event]
  (let [bitmap (:window/bitmap db)
        size (:window/size db)
        [x y] (matrix/add mouse-pos [(:x content-rect) (:y content-rect)])
        color (get-pixel-color bitmap (:width size) x y)]
    (assoc db :overlay [:div 
                        [:div {:style {:width "110px" :height "110px" :display "flex" :flex-wrap "wrap"}}
                         (map (fn [offset-y]
                                (map (fn [offset-x]
                                       [:div {:style {:width "10px"
                                                      :height "10px"
                                                      :box-sizing "border-box"
                                                      :border (when (and (= offset-x 0) (= offset-y 0)) (str "1px solid " styles/accent))
                                                      :background-color (gcolor/rgbArrayToHex (clj->js (get-pixel-color bitmap (:width size) (+ x offset-x) (+ y offset-y))))}}]) (range -5 6))) (range -5 6))]
                        [:div {:style {:display "flex" :padding-top "12px"}}
                         [:div {:style {:width "24px" :height "24px" :background-color (tools/rgba color)}}]
                         [:span {:style {:line-height "24px" :margin-left "12px"}} (gcolor/rgbArrayToHex (clj->js color))]]])))



