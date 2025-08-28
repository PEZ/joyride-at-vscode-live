(ns lm.human-intelligence
  (:require [promesa.core :as p]
            ["vscode" :as vscode]))

(defn ask!+
  "Shows a VS Code quick pick with provided
   * `question` as `title` (be brief)
   * `question-elaboration` as `placeHolder` (be brief)
   * `items` QuickPickItems
   * an optional keyword arg `:canSelectMany true`

   Returns selected item(s) or `:hi/cancelled` if cancelled or :hi/timeout if timed out."
  [question question-elaboration items & {:keys [canSelectMany]}]
  (let [quick-pick (vscode/window.createQuickPick)
        !state (atom {})]

    (set! (.-title quick-pick) question)
    (set! (.-placeholder quick-pick) question-elaboration)
    (set! (.-ignoreFocusOut quick-pick) true)
    (set! (.-items quick-pick) (clj->js (conj items {:label "Other ..."})))
    (when canSelectMany
      (set! (.-canSelectMany quick-pick) true))

    (.show quick-pick)

    (p/create
     (fn [resolve _reject]
       (swap! !state assoc ::timeout-id (js/setTimeout
                                         (fn []
                                           (swap! !state assoc ::timed-out true)
                                           (.dispose quick-pick))
                                         3000))
       (js/setTimeout (fn []
                        (println "ready")
                        (swap! !state assoc ::ready? true))
                      32)
       (.onDidAccept quick-pick
                     (fn []
                       (swap! !state assoc ::selected-items (seq (js->clj (.-selectedItems quick-pick)
                                                                          :keywordize-keys true)))
                       (.dispose quick-pick)))
       (.onDidChangeActive quick-pick
                           (fn []
                             (when (::ready? @!state)
                               (js/clearTimeout (::timeout-id @!state)))))
       (.onDidHide quick-pick
                   (fn []
                     (js/clearTimeout (::timeout-id @!state))
                     (if-not (some #{"Other ..."} (map :label (::selected-items @!state)))
                       (resolve
                        (cond
                          (::timed-out @!state) :hi/timeout
                          (::selected-items @!state) (::selected-items @!state)
                          :else :hi/cancelled))
                       (p/let [other (vscode/window.showInputBox #{:title question
                                                                   :placeholder question-elaboration})]
                         (resolve (conj (::selected-items @!state) other))))))))))

(comment
  (p/let [answer (ask!+ "hello" "because" [{:label "foo"}

                                           {:label "bar"}
                                           {:label "baz"
                                            :description "elaborate on baz"}]
                        :canSelectMany true)]
    (def answer answer))
  :rcf)

