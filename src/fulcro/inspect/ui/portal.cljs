(ns fulcro.inspect.ui.portal
  (:require [fulcro.client.primitives :as fp]
            [fulcro.client.dom :as dom]
            [goog.object :as gobj]
            [goog.dom :as gdom]
            [goog.style :as style]))

(defn render-subtree-into-container [parent c node]
  (let [c (if (= "function" (gobj/get c "type"))
            (js/React.cloneElement c)
            c)]
    (js/ReactDOM.unstable_renderSubtreeIntoContainer parent c node)))

(defn $ [s] (.querySelector js/document s))

(defn read-target [target]
  (cond
    (fn? target) (target)
    (nil? target) js/document.body
    (string? target) ($ target)
    :else target))

(defn create-portal-node [props]
  (let [node (doto (gdom/createElement "div")
               (style/setStyle (clj->js (:style props))))]
    (js/console.log "portal target" (read-target (:append-to props)))
    (cond
      (:append-to props) (gdom/append (read-target (:append-to props)) node)
      (:insert-after props) (gdom/insertSiblingAfter node (read-target (:insert-after props))))
    node))

(defn portal-render-children [children]
  (apply dom/div nil children))

(fp/defsc Portal [this _]
  {:componentDidMount
   (fn []
     (let [props (fp/props this)
           node (create-portal-node props)]
       (gobj/set this "node" node)
       (render-subtree-into-container this (portal-render-children (fp/children this)) node)))

   :componentWillUnmount
   (fn []
     (when-let [node (gobj/get this "node")]
       (js/ReactDOM.unmountComponentAtNode node)
       (gdom/removeNode node)))

   :componentDidUpdate
   (fn [_ _]
     (let [node (gobj/get this "node")]
       (render-subtree-into-container this (portal-render-children (fp/children this)) node)))}

  (dom/noscript nil))

(def portal (fp/factory Portal))
