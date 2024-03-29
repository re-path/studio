(ns renderer.events
  (:require
   [malli.core :as ma]
   [platform]
   [re-frame.core :as rf]
   [renderer.db :as db]
   [renderer.frame.handlers :as frame-h]
   [renderer.handlers :as h]
   [renderer.tools.base :as tools]
   [renderer.utils.local-storage :as local-storage]
   [renderer.utils.pointer :as pointer]))

(defn check-and-throw
  "Throws an exception if `db` doesn't match the Spec"
  [spec db]
  (when-not (ma/validate spec db)
    (js/console.log (ex-info (str "spec check failed: " (ma/explain spec db)) {}))
    db))

#_:clj-kondo/ignore
(def schema-valdator (rf/after (partial check-and-throw db/app)))

#_(rf/reg-global-interceptor schema-valdator)

(rf/reg-event-db
 :initialize-db
 (fn [_ _]
   db/default))

(rf/reg-event-db
 :load-local-db
 local-storage/persist
 (fn [db _]
   db))

(rf/reg-event-db
 :set-system-fonts
 (fn [db [_ fonts]]
   (assoc db :system-fonts fonts)))

(rf/reg-event-db
 :set-webref-css
 (fn [db [_ webref-css]]
   (assoc db :webref-css webref-css)))

(rf/reg-event-db
 :set-mdn
 (fn [db [_ mdn]]
   (assoc db :mdn mdn)))

(rf/reg-event-db
 :set-tool
 (fn [db [_ tool]]
   (tools/set-tool db tool)))

(rf/reg-event-db
 :clear-restored
 (fn [db [_]]
   (dissoc db :restored?)))

(rf/reg-event-db
 :set-lang
 (fn [db [_ lang]]
   (assoc db :lang lang)))

(rf/reg-event-db
 :set-repl-mode
 (fn [db [_ mode]]
   (assoc db :repl-mode mode)))

(rf/reg-event-db
 :toggle-debug-info
 (fn [db [_]]
   (update db :debug-info? not)))

(rf/reg-event-db
 :set-backdrop
 (fn [db [_ backdrop?]]
   (assoc db :backdrop? backdrop?)))

(rf/reg-event-db
 :toggle-rulers
 (fn [db [_]]
   (update db :rulers? not)))

(rf/reg-event-db
 :toggle-rulers-locked
 (fn [db [_]]
   (update db :rulers-locked? not)))

(rf/reg-event-db
 :toggle-grid
 (fn [db [_]]
   (update db :grid? not)))

#_:clj-kondo/ignore
(rf/reg-event-db
 :toggle-snap
 (fn [db [_]]
   (update db :snap? not)))

(rf/reg-event-db
 :pointer-event
 (fn [{:keys [pointer-offset tool dom-rect drag?] :as db}
      [_ {:keys [button buttons modifiers data-transfer pointer-pos delta element] :as e}]]
   (let [adjusted-pointer-pos (frame-h/adjusted-pointer-pos db pointer-pos)]
     (case (:type e)
       :pointermove
       (if (= buttons :right)
         db
         (-> (if pointer-offset
               (if (pointer/significant-drag? pointer-pos pointer-offset)
                 (cond-> db
                   (not= tool :pan)
                   (frame-h/pan-out-of-canvas dom-rect
                                              pointer-pos
                                              pointer-offset)

                   (not drag?)
                   (-> (tools/drag-start e element)
                       (assoc :drag? true))

                   :always
                   (tools/drag e element))
                 db)
               (tools/pointer-move db e element))
             (assoc :pointer-pos pointer-pos
                    :adjusted-pointer-pos adjusted-pointer-pos)))

       :pointerdown
       (cond-> db
         (= button :middle)
         (-> (assoc :primary-tool tool)
             (tools/set-tool :pan))

         (and (= button :right) (not= (:key element) :bounding-box))
         (tools/pointer-up e element)

         :always
         (-> (tools/pointer-down e element)
             (assoc :pointer-offset pointer-pos
                    :adjusted-pointer-offset adjusted-pointer-pos)))

       :pointerup
       (cond-> (if drag?
                 (tools/drag-end db e element)
                 (cond-> db (not= button :right) (tools/pointer-up e element)))
         (and (:primary-tool db) (= button :middle))
         (-> (tools/set-tool (:primary-tool db))
             (dissoc :primary-tool))

         :always
         (dissoc :pointer-offset :drag?))

       :dblclick
       (tools/double-click db e element)

       :wheel
       (if (some modifiers [:ctrl :alt])
         (let [delta-y (second delta)
               factor (Math/pow (inc (/ (- 1 (:zoom-sensitivity db)) 100))
                                (- delta-y))]
           (frame-h/zoom-in-pointer-position db factor))
         (frame-h/pan db delta))

       :drop
       (let [items (.-items data-transfer)
             files (.-files data-transfer)]
         (-> db
             (assoc :pointer-pos pointer-pos
                    :adjusted-pointer-pos adjusted-pointer-pos)
             (cond->
              items (h/drop-items items)
              files (h/drop-files files))))

       db))))

(rf/reg-event-db
 :keyboard-event
 (fn [{:keys [tool] :as db} [_ {:keys [type code] :as e}]]
   (case type
     :keydown
     (cond-> db
       (and (= code "Space")
            (not= tool :pan))
       (-> (assoc :primary-tool tool)
           (tools/set-tool :pan))

       :always
       (tools/key-down e))

     :keyup
     (cond-> db
       (and (= code "Space")
            (:primary-tool db))
       (-> (tools/set-tool (:primary-tool db))
           (dissoc :primary-tool))

       :always
       (tools/key-up e))

     db)))

(rf/reg-fx
 :send-to-main
 (fn [data]
   (when platform/electron?
     (js/window.api.send "toMain" (clj->js data)))))

(rf/reg-fx
 :clipboard-write
 (fn [text-html]
   (js/navigator.clipboard.write
    (clj->js [(js/ClipboardItem.
               #js {"text/html" (when text-html
                                  (js/Blob.
                                   [text-html]
                                   #js {:type ["text/html"]}))})]))))
