(ns renderer.tools.shape.circle
  "https://www.w3.org/TR/SVG/shapes.html#CircleElement"
  (:require
   [clojure.core.matrix :as mat]
   [clojure.string :as str]
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as attr.hierarchy]
   [renderer.element.handlers :as element.h]
   [renderer.tools.base :as tools]
   [renderer.tools.overlay :as overlay]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.units :as units]))

(derive :circle ::tools/shape)

(defmethod tools/properties :circle
  []
  {:icon "circle-alt"
   :description "The <circle> SVG element is an SVG basic shape, used to draw 
                 circles based on a center point and a radius."
   :attrs [:stroke-width
           :opacity
           :fill
           :stroke
           :stroke-dasharray]})

(defmethod tools/drag :circle
  [{:keys [adjusted-pointer-offset active-document adjusted-pointer-pos] :as db}]
  (let [{:keys [stroke fill]} (get-in db [:documents active-document])
        [offset-x offset-y] adjusted-pointer-offset
        radius (mat/distance adjusted-pointer-pos adjusted-pointer-offset)
        attrs {:cx offset-x
               :cy offset-y
               :fill fill
               :stroke stroke
               :r radius}]
    (element.h/set-temp db {:type :element :tag :circle :attrs attrs})))

(defmethod tools/translate :circle
  [element [x y]] (-> element
                      (attr.hierarchy/update-attr :cx + x)
                      (attr.hierarchy/update-attr :cy + y)))

(defmethod tools/scale :circle
  [el ratio pivot-point]
  (let [dimentions (bounds/->dimensions (tools/bounds el))
        pivot-point (mat/sub pivot-point (mat/div dimentions 2))
        offset (mat/sub pivot-point (mat/mul pivot-point ratio))
        ratio (apply min ratio)]
    (-> el
        (attr.hierarchy/update-attr :r * ratio)
        (tools/translate offset))))

(defmethod tools/bounds :circle
  [{{:keys [cx cy r]} :attrs}]
  (let [[cx cy r] (map units/unit->px [cx cy r])]
    [(- cx r) (- cy r) (+ cx r) (+ cy r)]))

(defmethod tools/area :circle
  [{{:keys [r]} :attrs}]
  (* Math/PI (Math/pow (units/unit->px r) 2)))

(defmethod tools/path :circle
  [{{:keys [cx cy r]} :attrs}]
  (let [[cx cy r] (map units/unit->px [cx cy r])]
    (str/join " " ["M" (+ cx r) cy
                   "A" r r 0 0 1 cx (+ cy r)
                   "A" r r 0 0 1 (- cx r) cy
                   "A" r r 0 0 1 (+ cx r) cy
                   "z"])))

(defmethod tools/edit :circle
  [el [x _y] handler]
  (case handler
    :r (attr.hierarchy/update-attr el :r #(abs (+ % x)))
    el))

(defmethod tools/render-edit :circle
  [{:keys [attrs key]}]
  (let [{:keys [cx cy r]} attrs
        [cx cy r] (mapv units/unit->px [cx cy r])
        active-page @(rf/subscribe [:element/active-page])
        page-pos (mapv
                  units/unit->px
                  [(-> active-page :attrs :x) (-> active-page :attrs :y)])
        [cx cy] (mat/add page-pos [cx cy])]
    [:g
     [overlay/line cx cy (+ cx r) cy]
     [overlay/label (str (units/->fixed r)) [(+ cx (/ r 2)) cy]]
     [overlay/times cx cy]
     [overlay/square-handler {:x (+ cx r)
                              :y cy
                              :key :r
                              :type :handler
                              :tag :edit
                              :element key}
      ^{:key (str key "-r")}
      [:title "r"]]]))