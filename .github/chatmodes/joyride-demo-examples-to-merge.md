# Detailed Joyride Demo Examples - For Merging into Live Demo Chatmode

This file contains the comprehensive examples that were removed from the general instructions and should be merged into the `joyride-live-demo-master.chatmode.md`.

## Complete Audio Workflow Examples

```clojure
(in-ns 'user)
(require '[ai-presenter.audio-generation :as audio]
         '[ai-presenter.audio-playback :as playback])

;; Initialize audio service (if not already active)
(playback/init-audio-service!)

;; Check audio system status
(playbook/get-audio-status!+)

;; Simple text-to-speech generation and immediate playback
(audio/generate-and-play-message!+ "Welcome to this Joyride demonstration!")

;; Generate audio file for a specific slide (saves to slides/voice/)
(audio/generate-slide-audio!+
  "welcome"
  "Welcome everyone! Today we're exploring Joyride.")

;; Load and play existing audio file
(playback/load-and-play-audio!+ "slides/voice/welcome.mp3")

;; Advanced audio control
(playback/play-audio!+)                    ; play current audio
(playback/pause-audio!+)                   ; pause playback
(playback/stop-audio!+)                    ; stop and reset
(playback/set-volume!+ 0.5)               ; set volume to 50%

;; Wait for audio completion (useful for sequencing)
(playback/play-and-wait-audio!+)

;; Check if user has enabled audio (required for browser security)
(playback/check-user-gesture!+)

;; Environment validation for TTS
(audio/validate-environment)
;; => {:api-key-present? true, :api-key-length 51}
```

## Enhanced Slide Navigation Examples

```clojure
(in-ns 'user)
(require 'next-slide)

;; Activate slide system
(next-slide/activate!)

;; Navigate slides
(next-slide/next! true)    ; forward
(next-slide/next! false)   ; backward
(next-slide/restart!)      ; go to first slide
(next-slide/current!)      ; refresh current slide

;; Show specific slide by name
(next-slide/show-slide-by-name!+ "welcome.md")

;; Get current slide info
(next-slide/get-current-slide-name+)  ; returns "welcome.md"

;; Check slide system state
@next-slide/!state
;; => {:next/active? true, :next/active-slide 0, :next/config-path ["slides.edn"]}

;; Deactivate slide system
(next-slide/deactivate!)
```

## Advanced Live Coding Demo Patterns

```clojure
(in-ns 'user)
;; Status bar item with animation
(def item (vscode/window.createStatusBarItem
           vscode/StatusBarAlignment.Right 1000))
(set! (.-text item) "🎸 Joyride")
(.show item)

;; Event handler with proper cleanup
(def disposable
  (vscode/workspace.onDidOpenTextDocument
    (fn [doc]
      (println "Opened:" (.-fileName doc)))))
;; Remember to dispose: (.dispose disposable)

;; Animated status bar with color cycling
(defn wave-alpha [alpha]
  (let [unit-alpha (/ alpha 255)
        cos-alpha (js/Math.cos (* js/Math.PI unit-alpha))
        shifted (/ (+ 1 cos-alpha) 2)]
    (* 255 shifted)))

(defn color-with-alpha [color alpha]
  (str color (-> alpha int js/Number. (.toString 16) (.padStart 2 "0"))))

(def gold "#FFD700")
(def !alpha (atom 0))

(defn nudge-color! []
  (let [alpha (wave-alpha @!alpha)]
    (swap! !alpha (partial + 15))
    (color-with-alpha gold alpha)))

(defn nudge-item! []
  (set! (.-color item) (nudge-color!)))

;; Start animation (brings back <blink>!)
(def interval-id (js/setInterval nudge-item! 16))

;; Stop animation
(js/clearInterval interval-id)

;; Configuration manipulation
(.update (vscode/workspace.getConfiguration "editor")
         "fontSize" 20 vscode/ConfigurationTarget.Global)

;; Extension API integration (Calva example)
(when-let [ext (vscode/extensions.getExtension "betterthantomorrow.calva")]
  (when (.-isActive ext)
    (let [calva (some-> ext .-exports .-v1)]
      ;; Use Calva's API for text replacement
      (p/let [[range _] (calva.ranges.currentTopLevelForm)]
        (calva.editor.replace vscode/window.activeTextEditor range "New text")))))

;; HTML to Hiccup conversion using NPM module
(require '["posthtml-parser" :as parser]
         '[clojure.walk :as walk])

(defn html->hiccup [html]
  (-> html
      (parser/parser)
      (js->clj :keywordize-keys true)
      (->> (into [:div])
           (walk/postwalk
            (fn [{:keys [tag attrs content] :as element}]
              (if tag
                (into [(keyword tag) (or attrs {})] content)
                element))))))

;; Find-and-replace with regex
(defn find-with-regex-on []
  (let [selection vscode/window.activeTextEditor.selection
        selected-text (.getText (.-document vscode/window.activeTextEditor) selection)
        escaped-text (-> selected-text
                         (.replace (js/RegExp. "[.?+*^$\\|(){}[\\]]" "g") "\\$&")
                         (.replace (js/RegExp. "\\n" "g") "\\n?$&"))]
    (vscode/commands.executeCommand "editor.actions.findWithArgs"
                                    #js {:isRegex true
                                         :searchString escaped-text})))
```

## Timer/Showtime Widget Examples

```clojure
(in-ns 'user)
(require 'showtime)

;; Start a timer widget in the status bar
(showtime/start!)

;; Stop the timer (keeps widget visible)
(showtime/stop!)

;; Check state
@showtime/!state

;; The timer shows elapsed time since start and is clickable
;; Demonstrates: status bar items, timers, command integration
```

## Workspace Automation Patterns

```clojure
(in-ns 'user)
;; Disposable management for re-runnable scripts
(defonce !db (atom {:disposables []}))

(defn clear-disposables! []
  (run! #(.dispose %) (:disposables @!db))
  (swap! !db assoc :disposables []))

(defn push-disposable [disposable]
  (swap! !db update :disposables conj disposable)
  (.push (.-subscriptions (joyride/extension-context)) disposable))

;; Evaluate clipboard content as Joyride code
(defn evaluate-clipboard+ []
  (p/let [clipboard-text (vscode/env.clipboard.readText)]
    (when (not-empty clipboard-text)
      (vscode/commands.executeCommand "joyride.runCode" clipboard-text))))

;; Auto-open workspace README as preview
(p/let [workspace-folder (first vscode/workspace.workspaceFolders)
        readme-path (vscode/Uri.joinPath (.-uri workspace-folder) "/README.md")]
  (vscode/commands.executeCommand "markdown.showPreview" readme-path))
```

## Advanced File and UI Operations

```clojure
(in-ns 'user)
;; Read workspace files with error handling
(p/let [files (vscode/workspace.findFiles "**/*.cljs")]
  (println "Found" (count files) "Clojure files"))

;; Interactive Quick Pick menu
(p/let [choice (vscode/window.showQuickPick
                 #js ["Save All" "Close All" "Toggle Sidebar"]
                 #js {:placeHolder "Choose action"})]
  (case choice
    "Save All" (vscode/commands.executeCommand "workbench.action.files.saveAll")
    "Close All" (vscode/commands.executeCommand "workbench.action.closeAllEditors")
    "Toggle Sidebar" (vscode/commands.executeCommand "workbench.action.toggleSidebarVisibility")
    nil))

;; Create interactive status bar button
(def button (vscode/window.createStatusBarItem vscode/StatusBarAlignment.Left))
(set! (.-text button) "🚀 Demo")
(set! (.-command button)
      #js {:command "joyride.runCode"
           :arguments ["(vscode/window.showInformationMessage \"Button clicked!\")"]})
(set! (.-tooltip button) "Click for demo")
(.show button)

;; File system operations
(p/let [workspace-root (-> vscode/workspace.workspaceFolders first .-uri)
        new-dir (vscode/Uri.joinPath workspace-root "demo-folder")
        _ (vscode/workspace.fs.createDirectory new-dir)
        demo-file (vscode/Uri.joinPath new-dir "demo.txt")
        content (js/TextEncoder. (.encode "Hello from Joyride!"))]
  (vscode/workspace.fs.writeFile demo-file content))
```

## Complete Demo Scenarios

### Coordinated Slide and Audio Presentation

```clojure
(in-ns 'user)
;; Complete presentation workflow
(require '[next-slide :as slides]
         '[ai-presenter.audio-generation :as audio]
         '[ai-presenter.audio-playback :as playback])

;; Start a presentation session
(slides/activate!)
(slides/restart!)  ; go to first slide

;; Generate and play audio for current slide
(p/let [slide-name (slides/get-current-slide-name+)
        slide-base (first (.split slide-name "."))]
  ;; Generate narration
  (audio/generate-slide-audio!+
    slide-base
    "Welcome to our interactive coding demonstration!")
  ;; Load and play the generated audio
  (playback/load-and-play-audio!+ (str "slides/voice/" slide-base ".mp3")))

;; Navigate with audio coordination
(p/let [_ (slides/next! true)  ; advance slide
        slide-name (slides/get-current-slide-name+)]
  (println "Now showing:" slide-name))
```

### Live Coding with Animation Demo

```clojure
(in-ns 'user)
;; Create an animated status bar item
(def demo-item (vscode/window.createStatusBarItem
                vscode/StatusBarAlignment.Right 100))

;; Set up animation state
(def !demo-state (atom {:alpha 0 :direction 1}))

(defn animate-demo-item! []
  (let [{:keys [alpha direction]} @!demo-state
        new-alpha (+ alpha (* direction 10))
        new-direction (cond
                        (>= new-alpha 255) -1
                        (<= new-alpha 0) 1
                        :else direction)
        final-alpha (max 0 (min 255 new-alpha))
        color (str "#FFD700" (-> final-alpha int (.toString 16) (.padStart 2 "0")))]
    (set! (.-text demo-item) "🎸 Live Demo")
    (set! (.-color demo-item) color)
    (swap! !demo-state assoc :alpha final-alpha :direction new-direction)))

;; Start demo animation
(def demo-timer (js/setInterval animate-demo-item! 50))
(.show demo-item)

;; Interactive demo button
(set! (.-command demo-item)
      #js {:command "joyride.runCode"
           :arguments ["(vscode/window.showInformationMessage \"Live coding is fun!\")"]})

;; Clean up demo
(js/clearInterval demo-timer)
(.dispose demo-item)
```

### Extension Integration Demo

```clojure
(in-ns 'user)
;; Demonstrate extension API integration
(when-let [calva-ext (vscode/extensions.getExtension "betterthantomorrow.calva")]
  (when (.-isActive calva-ext)
    (let [calva-api (some-> calva-ext .-exports .-v1)]
      ;; Show current REPL connection status
      (println "Calva REPL status:"
               (if calva-api "Connected" "API not available"))

      ;; Example of using Calva's API for code manipulation
      (p/let [[range _] (calva-api.ranges.currentTopLevelForm)]
        (println "Current top-level form range:" range)))))

;; Show workspace-specific information
(let [workspace-info {:folders (mapv #(.-name %) vscode/workspace.workspaceFolders)
                      :active-file (some-> vscode/window.activeTextEditor
                                           .-document .-fileName)
                      :language (some-> vscode/window.activeTextEditor
                                        .-document .-languageId)}]
  (vscode/window.showInformationMessage
    (str "Workspace: " (pr-str workspace-info))))
```

## REPL-First Development Pattern Examples

```clojure
(in-ns 'user)
;; Test VS Code API availability
(require '["vscode" :as vscode])
(some-> vscode/window.activeTextEditor .-document .-fileName)

;; Validate configuration access
(-> (vscode/workspace.getConfiguration "editor")
    (.get "fontSize"))

;; Test extension API before using
(when-let [ext (vscode/extensions.getExtension "betterthantomorrow.calva")]
  {:active (.-isActive ext)
   :api-available (some? (.-exports ext))})

;; Explore what's available
(keys (js->clj vscode/window))  ; What window methods exist?

;; Test specific functionality
(vscode/window.showInformationMessage "Testing..." "OK" "Cancel")
;; ^ This shows I need awaitResult: true to get the button choice
```

## Testing and Validation Examples

```clojure
(in-ns 'user)
;; Test small expressions first
(vscode/window.showInformationMessage "Hello from REPL!")

;; Verify object properties and methods exist
(js->clj (js/Object.keys vscode/window))

;; Check async operations behavior
(p/let [result (vscode/window.showInputBox #js {:prompt "Test input"})]
  (println "User entered:" result))

;; Confirm extension API availability
(map #(.-id %) vscode/extensions.all)
```

---

**Note**: All these examples should be integrated into the chatmode with proper organization and user involvement protocols.