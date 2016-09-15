 (ns foo.example.select
  (:require [reagent.core :as r :refer [atom]]
            cljsjs.react-autosuggest
            cljsjs.react-select
            [clojure.string :as str]))

(defn str->regex [a-str]
  (let [escaped (str/replace a-str #"[\+\.\?\[\]\(\)\^\$]" (partial str "\\"))]
    (re-pattern (str "(?i)^" escaped ".*"))))

(defn getSuggestions [val source]
  (let [trimmed-val (if (string? val) (str/trim val) "")]
   (if (empty? trimmed-val)
      []
      (into [] (filter (comp #(re-matches  (str->regex trimmed-val) %) :name) source)))))

(defn getSuggestionValue [suggestion]
  (.-name suggestion))

(defn  renderSuggestion [suggestion]
  ;(js/console.log suggestion)
  (r/as-element
    [:span (.-name suggestion)]))



(def Autosuggest (r/adapt-react-class js/Autosuggest))

(defn auto-suggest [id source sel-data]
  (let [suggestions (r/atom (getSuggestions "" ""))
        as-val (r/atom "")
        update-suggestions (fn [arg]
                             (let [new-sugg (getSuggestions (.-value arg) source)]
                               (reset! suggestions new-sugg)
                               nil))
        update-state-val (fn [evt new-val method]
                           (reset! as-val  (.-newValue new-val))
                           nil)]
    (fn []
      [Autosuggest {:id id
                    :suggestions @suggestions
                    :onSuggestionsUpdateRequested update-suggestions
                    :getSuggestionValue getSuggestionValue
                    :renderSuggestion renderSuggestion
                    :onSuggestionSelected (fn [_ x]  (sel-data (.-suggestionValue x)))
                    :inputProps {:placeholder "Unesi oznaku JUS-a"
                                 :value @as-val
                                 :onChange update-state-val}}])))


;; -------------------------
;; Views





;(defn mount-root []
;  (r/render [home-page] (.getElementById js/document "app")))


