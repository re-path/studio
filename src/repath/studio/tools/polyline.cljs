(ns repath.studio.tools.polyline
  (:require [repath.studio.elements.handlers :as elements]
            [repath.studio.tools.base :as tools]
            [repath.studio.units :as units]
            [repath.studio.attrs.views :as attrs]
            [repath.studio.history.handlers :as history]
            [clojure.string :as str]))

(derive :polyline ::tools/shape)

(defmethod tools/properties :polyline [] {:icon "polyline"
                                          :description "The <polyline> SVG element is an SVG basic shape that creates straight lines connecting several points."
                                          :attrs [:stroke-width
                                                  :fill
                                                  :stroke
                                                  :stroke-linejoin
                                                  :opacity]})

(defmethod tools/mouse-up :polyline
  [{:keys [active-document adjusted-mouse-pos] :as db} event]
  (let [temp-element (get-in db [:documents active-document :temp-element])
        stroke (get-in db [:documents active-document :stroke])]
   (if temp-element
     (if (= (:button event) 2)
       (-> db
           (elements/create-from-temp)
           (history/finalize (str "Create " (name (:type temp-element)))))
       (update-in db [:documents active-document :temp-element :attrs :points] #(str % " " (str/join " " adjusted-mouse-pos))))
     (-> db
         (assoc :state :create)
         (elements/set-temp {:type :polyline :attrs {:points (str/join " " adjusted-mouse-pos)
                                                     :stroke (tools/rgba stroke)
                                                     :fill "transparent"}})))))

(defmethod tools/drag-end :polyline
  [{:keys [active-document adjusted-mouse-pos adjusted-mouse-offset] :as db}]
  (let [stroke (get-in db [:documents active-document :stroke])]
    (if (get-in db [:documents active-document :temp-element])
      (update-in db [:documents active-document :temp-element :attrs :points] #(str % " " (str/join " " adjusted-mouse-pos)))
      (-> db
          (assoc :state :create)
          (elements/set-temp {:type :polyline :attrs {:points (str/join " " (concat adjusted-mouse-pos adjusted-mouse-offset))
                                                      :stroke (tools/rgba stroke)
                                                      :fill "transparent"}})))))

(defmethod tools/drag :polyline
  [{:keys [adjusted-mouse-offset active-document adjusted-mouse-pos] :as db} event element]
  (let [stroke (get-in db [:documents active-document :stroke])
        points (get-in db [:documents active-document :temp-element :attrs :points])
        attrs {:points (str/join " " (concat adjusted-mouse-pos adjusted-mouse-offset))
               :stroke (tools/rgba stroke)
               :fill "transparent"}]
    (if points
      (tools/mouse-move db event element)
      (-> db
          (assoc :state :create)
          (elements/set-temp {:type :polyline :attrs attrs})))))

(defmethod tools/mouse-move :polyline
  [{:keys [active-document adjusted-mouse-pos] :as db}]
  (let [points (get-in db [:documents active-document :temp-element :attrs :points])]
    (if points
      (assoc-in db [:documents active-document :temp-element :attrs :points] (str/join " " (concat (apply concat (drop-last (attrs/points-to-vec points))) adjusted-mouse-pos)))
      db)))

(defmethod tools/translate :polyline
  [element [x y]] (-> element
                      (update-in [:attrs :points] (fn [val]
                                                    (->> val
                                                         (attrs/points-to-vec)
                                                         (reduce (fn [points point] (concat points [(units/transform + x (first point)) (units/transform + y (second point))])) [])
                                                         (concat)
                                                         (str/join " "))))))

(defmethod tools/bounds :polyline
  [{{:keys [points]} :attrs}]
  (let [points-v (attrs/points-to-vec points)
        x1 (apply min (map #(units/unit->px (first %)) points-v))
        y1 (apply min (map #(units/unit->px (second %)) points-v))
        x2 (apply max (map #(units/unit->px (first %)) points-v))
        y2 (apply max (map #(units/unit->px (second %)) points-v))]
    [x1 y1 x2 y2]))

(defmethod tools/path :line
  [{{:keys [points]} :attrs}]
  (reduce #(str %1 "M " (str/join "," %2)) "" (attrs/points-to-vec points)))
