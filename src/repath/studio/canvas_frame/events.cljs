(ns repath.studio.canvas-frame.events
  (:require
   [re-frame.core :as rf]
   [repath.studio.canvas-frame.handlers :as h]
   [repath.studio.tools.base :as tools]
   [clojure.core.matrix :as matrix]
   [repath.studio.elements.handlers :as el]
   [repath.studio.units :as units]))

(rf/reg-event-db
 :canvas/resize
 (fn [{content-rect :content-rect :as db} [_ updated-content-rect]]
   (let [offset (-> (merge-with - content-rect updated-content-rect)
                    (select-keys [:width :height]))
         pan (matrix/div [(:width offset) (:height offset)] 2)]
     (-> db
         (assoc :content-rect updated-content-rect)
         (h/pan pan)))))

(rf/reg-event-db
 :pan
 (fn [db [_ [x y]]]
   (h/pan db [x y])))

(rf/reg-event-db
 :pan-to-element
 (fn [db [_ key]]
   (h/pan-to-element db key)))

(rf/reg-event-db
 :pan-to-active-page
 (fn [{:keys [active-document content-rect] :as db}  [_ zoom]]
   (let [active-page (get-in db [:documents active-document :active-page])
         {:keys [width height]} (:attrs (el/get-element db active-page))
         [width height] (map units/unit->px [width height])
         width-ratio (/ (:width content-rect) width)
         height-ratio (/ (:height content-rect) height)]
     (-> db
         (assoc-in [:documents active-document :zoom] (case zoom
                                                        :original 1
                                                        :fit (min width-ratio height-ratio)
                                                        :fill (max width-ratio height-ratio)))
         (h/pan-to-element active-page)))))

(rf/reg-event-db
 :zoom
 (fn [db [_ factor]]
   (h/zoom db factor)))

(rf/reg-event-db
 :set-zoom
 (fn [{active-document :active-document :as db} [_ zoom]]
   (let [current-zoom (get-in db [:documents active-document :zoom])]
     (h/zoom db (/ zoom current-zoom)))))

(rf/reg-event-db
 :zoom-in
 (fn [db [_ _]]
   (h/zoom db (/ 1 (:zoom-factor db)))))

(rf/reg-event-db
 :zoom-out
 (fn [db [_ _]]
   (h/zoom db (:zoom-factor db))))

(defn calc-pan-offset
  [mouse-pos size]
  (let [multiplier 0.1]
    (cond
      (< mouse-pos 0) (* mouse-pos multiplier)
      (> mouse-pos size) (* (- mouse-pos size) multiplier)
      :else 0)))

(defn pan-out-of-canvas
  [db {:keys [width height]} [x y]]
  (h/pan db [(calc-pan-offset x width) (calc-pan-offset y height)]))

(rf/reg-event-db
 :mouse-event
 (fn [{:keys [mouse-offset tool content-rect] :as db} [_ event]]
   (let [{:keys [mouse-pos delta element]} event
         adjusted-mouse-pos (h/adjusted-mouse-pos db mouse-pos)]
     (case (:type event)
       :mousemove
       (-> (if mouse-offset
             (cond-> db
               (not= tool :pan) (pan-out-of-canvas content-rect mouse-pos)
               (not (:drag-started db)) (-> (tools/drag-start event element)
                                            (assoc :drag-started true))
               :always (tools/drag event element))
             (tools/mouse-move db event element))
           (assoc :mouse-pos mouse-pos
                  :adjusted-mouse-pos adjusted-mouse-pos))

       :mousedown
       (cond-> db
         (= (:button event) 1) (-> (assoc :cached-tool tool)
                                   (tools/set-tool :pan))
         :always (-> (tools/mouse-down event element)
                     (assoc :drag-started false
                            :mouse-offset mouse-pos
                            :adjusted-mouse-offset adjusted-mouse-pos)))

       :mouseup
       (cond-> (if (:drag-started db)
                 (tools/drag-end db event element)
                 (tools/mouse-up db event element))
         (:cached-tool db) (tools/set-tool (:cached-tool db))
         :always (dissoc :cached-tool :mouse-offset))

       :dblclick (tools/double-click db event element)

       :wheel
       (if (some (:modifiers event) [:ctrl :alt])
         (let [factor (if (pos? (second delta)) (:zoom-factor db) (/ 1 (:zoom-factor db)))]
           (h/zoom-in-mouse-position db factor))
         (h/pan db delta))
       db))))
