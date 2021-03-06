(ns repath.studio.codemirror.views
  (:require [reagent.core :as ra]
            [reagent.dom :as dom]
            ["codemirror" :as codemirror]
            ["codemirror/mode/css/css.js"]
            ["codemirror/mode/xml/xml.js"]
            ["codemirror/addon/hint/show-hint.js"]
            ["codemirror/addon/hint/css-hint.js"]))

(def default-options {:lineNumbers false
                      :matchBrackets true
                      :lineWrapping true
                      :styleActiveLine true
                      :tabMode "spaces"
                      :autofocus false
                      :extraKeys {"Ctrl-Space" "autocomplete"}
                      :theme "tomorrow-night-eighties"
                      :autoCloseBrackets true
                      :mode "css"})

(defn editor
  [value {:keys [style options on-init on-blur]}]
  (let [cm (ra/atom nil)]
    (ra/create-class
     {:component-did-mount
      (fn [this]
        (let [el (dom/dom-node this)]

          (reset! cm (.fromTextArea codemirror el (clj->js (merge default-options options))))

          (.setValue @cm value)
          
          ;; Line up wrapped text with the base indentation.
          ;; SEE https://codemirror.net/demo/indentwrap.html
          (.on @cm "renderLine" (fn [editor line elt]
                                  (let [off (* (.countColumn codemirror (.-text line) nil (.getOption editor "tabSize")) (.defaultCharWidth @cm))]
                                    (set! (.. elt -style -textIndent) (str "-" off "px"))
                                    (set! (.. elt -style -paddingLeft) (str (+ 4 off) "px")))))

          (.refresh @cm)

          (when on-blur
            (.on @cm "blur" #(on-blur (.getValue @cm))))

          (when on-init
            (on-init @cm))))

      :component-will-unmount
      (fn []
        (when @cm (reset! cm nil)))

      :component-did-update
      (fn [this _]
        (let [value (second (ra/argv this))]
          (.setValue @cm value)))

      :reagent-render
      (fn [value]
        [:textarea {:value value :style style :on-blur #() :on-change #()}])})))
