(ns fulcro.inspect.ui.multi-inspector
  (:require
    [cljs.reader :refer [read-string]]
    [fulcro-css.css :as css]
    [fulcro.client.mutations :as mutations]
    [fulcro.inspect.ui.core :as ui]
    [fulcro.inspect.ui.inspector :as inspector]
    [fulcro.client.dom :as dom]
    [fulcro.client.primitives :as fp]
    [fulcro.inspect.helpers :as h]
    [fulcro.inspect.ui.events :as events]
    [fulcro.inspect.helpers :as db.h]
    [garden.core :as g]))

(mutations/defmutation add-inspector [inspector]
  (action [env]
    (let [{:keys [ref state]} env
          inspector-ref (fp/ident inspector/Inspector inspector)
          current       (get-in @state (conj ref ::current-app))]
      (swap! state h/merge-entity inspector/Inspector inspector :append (conj ref ::inspectors))
      (if (nil? current)
        (swap! state update-in ref assoc ::current-app inspector-ref)))))

(mutations/defmutation set-app [{::inspector/keys [id]}]
  (action [env]
    (let [{:keys [ref state]} env]
      (swap! state update-in ref assoc ::current-app [::inspector/id id]))))

(fp/defsc MultiInspector [this {::keys [inspectors current-app] :as props} _ css]
  {:initial-state (fn [_] {::inspectors  []
                           ::current-app nil})

   :ident         (fn [] [::multi-inspector "main"])
   :query         [::inspectors
                   {[:fulcro.inspect.core/floating-panel "main"] [:ui/visible?]}
                   {::current-app (fp/get-query inspector/Inspector)}]
   :css           [[:.container {:display        "flex"
                                 :flex-direction "column"
                                 :width          "100%"
                                 :height         "100%"
                                 :overflow       "hidden"}]
                   [:.selector {:font-family ui/label-font-family
                                :font-size   ui/label-font-size
                                :display     "flex"
                                :align-items "center"
                                :background  "#f3f3f3"
                                :color       ui/color-text-normal
                                :border-top  "1px solid #ccc"
                                :padding     "12px"
                                :user-select "none"}]
                   [:.label {:margin-right "10px"}]
                   [:.no-app {:display         "flex"
                              :background      "#f3f3f3"
                              :font-family     ui/label-font-family
                              :font-size       "23px"
                              :flex            1
                              :align-items     "center"
                              :justify-content "center"}]]
   :css-include   [inspector/Inspector]}

  (let [keystroke (or (fp/shared this [:options :launch-keystroke]) "ctrl-f")
        {:ui/keys [visible?]} (get props [:fulcro.inspect.core/floating-panel "main"])]
    (dom/div #js {:className (:container css)}
      (events/key-listener {::events/action    #(fp/transact! (fp/get-reconciler this) [:fulcro.inspect.core/floating-panel "main"]
                                                  `[(db.h/persistent-set-props {::db.h/local-key   :ui/visible?
                                                                                ::db.h/storage-key :fulcro.inspect.core/dock-visible?
                                                                                ::db.h/value       ~(not visible?)}) :ui/visible?])
                            ::events/keystroke keystroke
                            ::events/target    #(.closest (dom/node this) "body")})
      (dom/style #js {:dangerouslySetInnerHTML #js {:__html (g/css [[:body {:margin "0" :padding "0" :box-sizing "border-box"}]])}})
      (dom/style #js {:dangerouslySetInnerHTML #js {:__html (g/css (css/get-css MultiInspector))}})
      (if current-app
        (inspector/inspector current-app)
        (dom/div #js {:className (:no-app css)}
          (dom/div nil "No app connected.")))
      (if (> (count inspectors) 1)
        (dom/div #js {:className (:selector css)}
          (dom/div #js {:className (:label css)} "App")
          (dom/select #js {:value    (pr-str (::inspector/id current-app))
                           :onChange #(fp/transact! this `[(set-app {::inspector/id ~(read-string (.. % -target -value))})])}
            (for [app-id (->> (map (comp pr-str second) inspectors) sort)]
              (dom/option #js {:key   app-id
                               :value app-id}
                app-id))))))))

(def multi-inspector (fp/factory MultiInspector {:keyfn ::multi-inspector}))
