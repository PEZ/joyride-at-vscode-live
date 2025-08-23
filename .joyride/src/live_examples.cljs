(ns live-examples
  (:require ["vscode" :as vscode]
            [joyride.core :as joy]
            [promesa.core :as p]))



(comment




  ;; Write to the Joyride Output channel
  (.appendLine (joy/output-channel) "Hello VS Code Live Stream Chat! ♥️")

  ;; Write to the REPL stdout
  (println "Hello Chat! ♥️")


  ;; Show information message











  ;; Config/settings
  ;; the Zen Mode Status bar
  (.get (vscode/workspace.getConfiguration "zenMode") "hideStatusBar")
  (.update (vscode/workspace.getConfiguration "zenMode")
           "hideStatusBar"
           (not (.get (vscode/workspace.getConfiguration "zenMode") "hideStatusBar"))
           vscode/ConfigurationTarget.Workspace)

  ;; Toggle editor line numbers
  (.get (vscode/workspace.getConfiguration "zenMode") "hideLineNumbers")
  (.get (vscode/workspace.getConfiguration "editor") "lineNumbers")

  (set! (.-lineNumbers vscode/window.activeTextEditor.options)
        ({1 0 0 1} (.-lineNumbers vscode/window.activeTextEditor.options)))














  ;; Create a statusbar item
  (def item (vscode/window.createStatusBarItem
             vscode/StatusBarAlignment.Right
             1000))

  (set! (.-text item) "0.1 + 0.2")

  (.show item)
  (.hide item)











  ;; We need a tooltip!
  (set! (.-tooltip item) "Educate yourself")

  ;; Make it a button
  (set! (.-command item)
        (clj->js
         {:command "joyride.runCode"
          :arguments [(str
                       '(:require '["vscode" :as vscode]
                                  '[joyride.core :as joy])
                       '(.appendLine (joy/output-channel)
                                     "Opening education")
                       '(vscode/commands.executeCommand
                         "simpleBrowser.show"
                         (str "https://" (+ 0.1 0.2) ".com")))]}))
  ;; Would be simpler to use the command `simpleBrowser.show` here
  ;; Except, the logging side effect

  (.show item)



  ;; Paint the item
  (def gold "#FFD700")

  (set! (.-color item) gold)


  ;; Update the color alpha

  (defonce !alpha (atom 240))

  (defn nudge-color! []
    (let [color gold
          alpha (* 255
                   (/ (+ (js/Math.cos (* 2
                                         js/Math.PI
                                         (/ @!alpha 255)))
                         1)
                      2))]
      (swap! !alpha (comp inc inc inc))
      (str color (-> alpha
                     int
                     js/Number.
                     (.toString 16)
                     (.padStart 2 "0")))))

  (nudge-color!)
  @!alpha
  (reset! !alpha 1)
  (reset! !alpha 64)
  (reset! !alpha 125)
  (reset! !alpha 191)
  (reset! !alpha 255)

  (defn nudge-item! []
    (set! (.-color item) (nudge-color!)))

  (nudge-item!)






  ;; Bring <blink> back!
  (defonce !interval-ids (atom []))
  (swap! !interval-ids conj (js/setInterval nudge-item! 20))
  (js/clearInterval (peek @!interval-ids))
  @!interval-ids
  (reset! !interval-ids [])



  (.dispose item)














  ;; Working with Extensio1n APIs

  (def calva-ext (vscode/extensions.getExtension "betterthantomorrow.calva"))
  (.-isActive calva-ext)
  (def calva (some-> calva-ext .-exports .-v1))

  (vscode/commands.executeCommand
   "simpleBrowser.show"
   "https://calva.io/api/#editorreplace")

  (-> (p/let [[top-level-form-range _] (calva.ranges.currentTopLevelForm)
              _ (calva.editor.replace
                 vscode/window.activeTextEditor
                 top-level-form-range
                 "Some new text")]
        (println "Text replaced!"))
      (p/catch (fn [e]
                 (println "Error replacing text:" e))))















  ;; Require from NPM

  (require '["posthtml-parser" :as parser]
           '[clojure.walk :as walk])

  (defn html->hiccup
    [html]
    (-> html
        (parser/parser)
        (js->clj :keywordize-keys true)
        (->> (into [:div])
             (walk/postwalk
              (fn [{:keys [tag attrs content] :as element}]
                (if tag
                  (into [(keyword tag) (or attrs {})] content)
                  element))))))

  (comment
    (def html "<label for=\"hw\">Foo</label><ul id=\"foo\"><li>Hello</li></ul>")
    (html->hiccup html)
    :rcf)

  (vscode/commands.executeCommand
   "simpleBrowser.show"
   "https://github.com/BetterThanTomorrow/joyride/blob/master/doc/api.md")

  (vscode/env.openExternal
   (vscode/Uri.parse
    "https://github.com/BetterThanTomorrow/joyride/blob/master/doc/api.md"))











  :rcf)














































































































































































































































































































































































































































































































































































































































































































































































































































































































;; ⠀⠀⠀⠀⠀⠀⠀⠀⠀⢀⣀⣠⣤⣤⣴⣦⣤⣤⣄⣀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
;; ⠀⠀⠀⠀⠀⠀⢀⣤⣾⣿⣿⣿⣿⠿⠿⠿⠿⣿⣿⣿⣿⣶⣤⡀⠀⠀⠀⠀⠀⠀
;; ⠀⠀⠀⠀⣠⣾⣿⣿⡿⠛⠉⠀⠀⠀⠀⠀⠀⠀⠀⠉⠛⢿⣿⣿⣶⡀⠀⠀⠀⠀
;; ⠀⠀⠀⣴⣿⣿⠟⠁⠀⠀⠀⣶⣶⣶⣶⡆⠀⠀⠀⠀⠀⠀⠈⠻⣿⣿⣦⠀⠀⠀
;; ⠀⠀⣼⣿⣿⠋⠀⠀⠀⠀⠀⠛⠛⢻⣿⣿⡀⠀⠀⠀⠀⠀⠀⠀⠙⣿⣿⣧⠀⠀
;; ⠀⢸⣿⣿⠃⠀⠀⠀⠀⠀⠀⠀⠀⢀⣿⣿⣷⠀⠀⠀⠀⠀⠀⠀⠀⠸⣿⣿⡇⠀
;; ⠀⣿⣿⡿⠀⠀⠀⠀⠀⠀⠀⠀⢀⣾⣿⣿⣿⣇⠀⠀⠀⠀⠀⠀⠀⠀⣿⣿⣿⠀
;; ⠀⣿⣿⡇⠀⠀⠀⠀⠀⠀⠀⢠⣿⣿⡟⢹⣿⣿⡆⠀⠀⠀⠀⠀⠀⠀⣹⣿⣿⠀
;; ⠀⣿⣿⣷⠀⠀⠀⠀⠀⠀⣰⣿⣿⠏⠀⠀⢻⣿⣿⡄⠀⠀⠀⠀⠀⠀⣿⣿⡿⠀
;; ⠀⢸⣿⣿⡆⠀⠀⠀⠀⣴⣿⡿⠃⠀⠀⠀⠈⢿⣿⣷⣤⣤⡆⠀⠀⣰⣿⣿⠇⠀
;; ⠀⠀⢻⣿⣿⣄⠀⠀⠾⠿⠿⠁⠀⠀⠀⠀⠀⠘⣿⣿⡿⠿⠛⠀⣰⣿⣿⡟⠀⠀
;; ⠀⠀⠀⠻⣿⣿⣧⣄⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣠⣾⣿⣿⠏⠀⠀⠀
;; ⠀⠀⠀⠀⠈⠻⣿⣿⣷⣤⣄⡀⠀⠀⠀⠀⠀⠀⢀⣠⣴⣾⣿⣿⠟⠁⠀⠀⠀⠀
;; ⠀⠀⠀⠀⠀⠀⠈⠛⠿⣿⣿⣿⣿⣿⣶⣶⣿⣿⣿⣿⣿⠿⠋⠁⠀⠀⠀⠀⠀⠀
;; ⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠉⠉⠛⠛⠛⠛⠛⠛⠉⠉⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀

"♥️ Hello Chat! ♥️"
