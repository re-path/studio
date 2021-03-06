(ns repath.studio.tools.animate
  (:require [repath.studio.tools.base :as tools]))

(derive :animate ::tools/animation)

(defmethod tools/properties :animate [] {:description "The SVG <animate> element provides a way to animate an attribute of an element over time."
                                         :attrs [:href
                                                 :attributeType
                                                 :attributeName
                                                 :begin
                                                 :end
                                                 :dur
                                                 :min
                                                 :max
                                                 :restart
                                                 :repeatCount
                                                 :repeatDur
                                                 :fill
                                                 :calcMode
                                                 :values
                                                 :keyTimes
                                                 :keySplines
                                                 :from
                                                 :to
                                                 :by
                                                 :autoReverse
                                                 :accelerate
                                                 :decelerate
                                                 :additive
                                                 :accumulate
                                                 :id
                                                 :class]})