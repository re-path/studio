(ns renderer.utils.keyboard
  (:require
   [re-frame.core :as rf]
   [clojure.set :as set])
  (:import
   [goog.events KeyCodes]))

(def key-codes
  "SEE https://google.github.io/closure-library/api/goog.events.KeyCodes.html"
  (js->clj KeyCodes))

(def key-chars
  (set/map-invert key-codes))

(defn code->key
  [code]
  (get key-chars code))

(def keydown-rules
  {:event-keys [[[:document/toggle-grid]
                 [{:keyCode 35}]
                 [{:keyCode (key-codes "THREE")
                   :shiftKey true}]]
                [[:element/raise]
                 [{:keyCode (key-codes "PAGE_UP")}]]
                [[:element/lower]
                 [{:keyCode (key-codes "PAGE_DOWN")}]]
                [[:element/raise-to-top]
                 [{:keyCode (key-codes "HOME")}]]
                [[:element/lower-to-bottom]
                 [{:keyCode (key-codes "END")}]]
                [[:pan-to-active-page :original]
                 [{:keyCode (key-codes "ONE")}]]
                [[:pan-to-active-page :fit]
                 [{:keyCode (key-codes "TWO")}]]
                [[:pan-to-active-page :fill]
                 [{:keyCode (key-codes "THREE")}]]
                [[:zoom-in]
                 [{:keyCode (key-codes "EQUALS")}]]
                [[:zoom-out]
                 [{:keyCode (key-codes "DASH")}]]
                [[:panel/toggle :tree]
                 [{:keyCode (key-codes "T")
                   :ctrlKey true}]]
                [[:element/->path]
                 [{:keyCode (key-codes "P")
                   :ctrlKey true
                   :shiftKey true}]]
                [[:element/stroke->path]
                 [{:keyCode (key-codes "P")
                   :ctrlKey true
                   :altKey true}]]
                [[:panel/toggle :properties]
                 [{:keyCode (key-codes "P")
                   :ctrlKey true}]]
                [[:window/toggle-header]
                 [{:keyCode (key-codes "H")
                   :altKey true
                   :ctrlKey true}]]
                [[:element/copy]
                 [{:keyCode (key-codes "C")
                   :ctrlKey true}]]
                [[:element/paste-styles]
                 [{:keyCode (key-codes "V")
                   :ctrlKey true
                   :shiftKey true}]]
                [[:element/paste-in-place]
                 [{:keyCode (key-codes "V")
                   :ctrlKey true
                   :altKey true}]]
                [[:element/paste]
                 [{:keyCode (key-codes "V")
                   :ctrlKey true}]]
                [[:element/cut]
                 [{:keyCode (key-codes "X")
                   :ctrlKey true}]]
                [[:toggle-debug-info]
                 [{:keyCode (key-codes "D")
                   :ctrlKey true
                   :shiftKey true}]]
                [[:element/duplicate-in-place]
                 [{:keyCode (key-codes "D")
                   :ctrlKey true}]]
                [[:element/bool-operation :exclude]
                 [{:keyCode (key-codes "E")
                   :ctrlKey true}]]
                [[:element/bool-operation :unite]
                 [{:keyCode (key-codes "U")
                   :ctrlKey true}]]
                [[:element/bool-operation :intersect]
                 [{:keyCode (key-codes "I")
                   :ctrlKey true}]]
                [[:element/bool-operation :subtract]
                 [{:keyCode (key-codes "S")
                   :ctrlKey true}]]
                [[:element/bool-operation :divide]
                 [{:keyCode (key-codes "/")
                   :ctrlKey true}]]
                [[:element/ungroup]
                 [{:keyCode (key-codes "G")
                   :ctrlKey true
                   :shiftKey true}]]
                [[:element/group]
                 [{:keyCode (key-codes "G")
                   :ctrlKey true}]]
                [[:element/unlock]
                 [{:keyCode (key-codes "L")
                   :ctrlKey true
                   :shiftKey true}]]
                [[:element/lock]
                 [{:keyCode (key-codes "L")
                   :ctrlKey true}]]
                [[:element/delete]
                 [{:keyCode (key-codes "DELETE")}]
                 [{:keyCode (key-codes "BACKSPACE")}]]
                [[:document/new]
                 [{:keyCode (key-codes "N")
                   :ctrlKey true}]]
                [[:history/cancel]
                 [{:keyCode (key-codes "ESC")}]]
                [[:history/redo]
                 [{:keyCode (key-codes "Z")
                   :ctrlKey true
                   :shiftKey true}]
                 [{:keyCode (key-codes "Y")
                   :ctrlKey true}]]
                [[:history/undo]
                 [{:keyCode (key-codes "Z")
                   :ctrlKey true}]]
                [[:element/select-same-tags]
                 [{:keyCode (key-codes "A")
                   :ctrlKey true
                   :shiftKey true}]]
                [[:element/select-all]
                 [{:keyCode (key-codes "A")
                   :ctrlKey true}]]
                [[:menubar/toggle "file"]
                 [{:keyCode (key-codes "F")
                   :altKey true}]]
                [[:menubar/toggle "edit"]
                 [{:keyCode (key-codes "E")
                   :altKey true}]]
                [[:menubar/toggle "object"]
                 [{:keyCode (key-codes "O")
                   :altKey true}]]
                [[:menubar/toggle "view"]
                 [{:keyCode (key-codes "V")
                   :altKey true}]]
                [[:menubar/toggle "help"]
                 [{:keyCode (key-codes "H")
                   :altKey true}]]
                [[:set-tool :edit]
                 [{:keyCode (key-codes "E")}]]
                [[:set-tool :circle]
                 [{:keyCode (key-codes "C")}]]
                [[:set-tool :line]
                 [{:keyCode (key-codes "L")}]]
                [[:set-tool :text]
                 [{:keyCode (key-codes "T")}]]
                [[:set-tool :pan]
                 [{:keyCode (key-codes "P")}]]
                [[:set-tool :zoom]
                 [{:keyCode (key-codes "Z")}]]
                [[:set-tool :rect]
                 [{:keyCode (key-codes "R")}]]
                [[:set-tool :select]
                 [{:keyCode (key-codes "S")}]]
                [[:set-tool :fill]
                 [{:keyCode (key-codes "F")}]]
                [[:element/move-up]
                 [{:keyCode (key-codes "UP")}]]
                [[:element/move-down]
                 [{:keyCode (key-codes "DOWN")}]]
                [[:element/move-left]
                 [{:keyCode (key-codes "LEFT")}]]
                [[:element/move-right]
                 [{:keyCode (key-codes "RIGHT")}]]
                [[:window/close]
                 [{:keyCode (key-codes "Q")
                   :ctrlKey true}]]
                [[:document/open]
                 [{:keyCode (key-codes "O")
                   :ctrlKey true}]]
                [[:document/save]
                 [{:keyCode (key-codes "S")
                   :ctrlKey true}]]
                [[:document/save-as]
                 [{:keyCode (key-codes "S")
                   :ctrlKey true
                   :shiftKey true}]]
                [[:document/save-all]
                 [{:keyCode (key-codes "S")
                   :ctrlKey true
                   :altKey true}]]
                [[:document/close-active]
                 [{:keyCode (key-codes "W")
                   :ctrlKey true}]]
                [[:document/close-all]
                 [{:keyCode (key-codes "W")
                   :ctrlKey true
                   :altKey true}]]
                [[:document/close-others]
                 [{:keyCode (key-codes "W")
                   :ctrlKey true
                   :shiftKey true}]]
                [[:window/toggle-fullscreen]
                 [{:keyCode (key-codes "F11")}]]
                [[:cmdk/toggle]
                 [{:keyCode (key-codes "K")
                   :ctrlKey true}]]]

   :clear-keys []

   :always-listen-keys []

   :prevent-default-keys [{:keyCode (key-codes "EQUALS")}
                          {:keyCode (key-codes "DASH")}
                          {:keyCode (key-codes "RIGHT")}
                          {:keyCode (key-codes "LEFT")}
                          {:keyCode (key-codes "UP")}
                          {:keyCode (key-codes "DOWN")}
                          {:keyCode (key-codes "F11")}
                          {:keyCode (key-codes "A")
                           :ctrlKey true}
                          {:keyCode (key-codes "P")
                           :ctrlKey true}
                          {:keyCode (key-codes "W")
                           :ctrlKey true}
                          {:keyCode (key-codes "K")
                           :ctrlKey true}
                          {:keyCode (key-codes "D")
                           :ctrlKey true
                           :shiftKey true}]})

(defn event-handler
  "SEE https://day8.github.io/re-frame/FAQs/Null-Dispatched-Events/"
  [event]
  (rf/dispatch-sync [:keyboard-event {:target (.-target event)
                                      :type (keyword (.-type event))
                                      :code (.-code event)
                                      :key-code (.-keyCode event)
                                      :key (.-key event)
                                      :modifiers (cond-> #{}
                                                   (.-altKey event) (conj :alt)
                                                   (.-ctrlKey event) (conj :ctrl)
                                                   (.-metaKey event) (conj :meta)
                                                   (.-shiftKey event) (conj :shift))}]))
