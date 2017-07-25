(ns maria.commands.which-key
  (:require [re-view.core :as v :refer [defview]]
            [re-db.d :as d]
            [maria.commands.registry :as registry]
            [maria.commands.exec :as exec]
            [clojure.set :as set]))

(defn show-keyset
  "Render a keyset. Does not support multi-step key-combos."
  [modifiers-down [keyset]]
  (let [keyset (set/difference keyset modifiers-down)
        sort-ks #(sort-by (fn [x] (if (string? x) x (:name (meta x)))) %)]
    [:.dib.bg-near-white.ph1.br2.pv05
     {:key (str keyset)}
     (->> (sort-ks (set/intersection keyset registry/modifiers))
          (map registry/show-key))
     " "
     (->> (sort-ks (set/difference keyset registry/modifiers))
          (map registry/show-key))]))

(defn show-hint [modifiers-down {:keys [display-name namespace doc bindings]}]
  [:.flex.items-center.ws-nowrap.mv1 display-name [:.flex-auto] (show-keyset modifiers-down (first bindings))])

(defn show-namespace-hints [modifiers-down [namespace hints]]
  (let [row-height 24]
    [:.c-avoid.bt.b--near-white.pv1
     [:.flex
      [:.pr1.flex-none.tr.b.pv2.flex.items-center.justify-end
       {:style {:min-width 70
                :height    row-height}}
       namespace]
      [:.flex-auto
       (for [{:keys [display-name name bindings]} hints]
         [:.flex.items-center.ws-nowrap.pointer.hover-bg-near-white.pl1.br2
          {:on-mouse-down #(exec/exec-command-name name)
           :style         {:height row-height}}
          display-name
          [:.flex-auto]
          (show-keyset modifiers-down (first bindings))])]]]))

(defview show-hints
  []
  (let [modifiers-down (d/get :commands :modifiers-down)]
    (if-let [hints (seq (exec/contextual-hints modifiers-down))]
      [:.fixed.bottom-0.left-0.right-0.z-999.bg-white.shadow-4.f7.sans-serif.ph2.hint-columns.overflow-auto
       {:style {:max-height 150}}
       (->> hints
            (group-by :namespace)
            (map (partial show-namespace-hints modifiers-down)))]
      [:.fixed])))