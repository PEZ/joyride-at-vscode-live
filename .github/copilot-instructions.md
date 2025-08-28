# Joyride Live Demo Supporting project

This is a **Joyride workspace automation project** demonstrating VS Code customization and presentation tooling using ClojureScript. The project showcases interactive programming with Joyride, where VS Code becomes scriptable in user space using the full VS Code Extension API. The project supports a streamed live demo of Joyride on the VS Code Youtube channel. Address the audience as "Chat".

## Essential Joyride Information Sources

**Always use these tools first** to get comprehensive, up-to-date information:

- `joyride_basics_for_agents` - Technical guide for LLM agents using Joyride evaluation capabilities
- `joyride_assisting_users_guide` - Complete user assistance guide with project structure, patterns, examples, and troubleshooting

These tools contain all the detailed information about Joyride APIs, project structure, common patterns, user workflows, and troubleshooting guidance.

## Project Architecture

### Core Components
- **Joyride Scripts**: Workspace-specific automation in `.joyride/` (version-controlled automation)
- **Presentation System**: Custom slideshow with navigation, notes, and audio playback (demo of Joyride capabilities, not the primary presentation format). Agents should understand audio generation for slides, ad-hoc speech, and coordinating `next-slide` with `audio-playback` namespaces
- **Interactive Examples**: Live coding demonstrations and REPL-driven development patterns
- **Audio Tooling**: TTS generation and webview-based audio playback system

### Key Technologies
- **Joyride**: VS Code extension providing ClojureScript runtime (SCI) in Extension Host
- **ClojureScript**: Functional programming with full VS Code API access
- **Promesa**: Promise handling for async VS Code operations
- **NPM Dependencies**: `ai-text-to-speech`, `posthtml-parser` for enhanced functionality

## Essential File Structure

```
.joyride/
├── scripts/
│   └── workspace_activate.cljs    # Project startup automation
├── src/
│   ├── live_examples.cljs         # Interactive coding examples
│   ├── next_slide.cljs           # Presentation navigation
│   ├── next_slide_notes.cljs     # Speaker notes management
│   ├── showtime.cljs             # Timer status bar widget
│   └── ai_presenter/             # Audio generation & playback
└── resources/
    └── audio-service.html        # Webview for browser audio

slides/                           # Markdown presentation content
slides/voice/                     # Audio files for slides
slides.edn                       # Slide deck configuration
next-slide.css                   # Presentation styling
```

## Development Patterns

### Interactive Programming Workflow
This project demonstrates **REPL-driven development**:
1. Use `joyride_evaluate_code` tool to test code before editing files
2. Develop incrementally - evaluate small expressions to build up solutions
3. Always verify API usage in the REPL before suggesting changes
4. Prefer pure functions over side effects

### Workspace Activation Pattern

When working with the workspace_activate.cljs script. Carefully study its patterns to understand why things are done the way they are.

### Disposable Management
Critical pattern for Joyride scripts, especially when experimenting in the REPL:
```clojure
;; Always register event handlers with extension context to prevent leaks
(push-disposable
  (vscode/workspace.onDidOpenTextDocument handler))
```
The `workspace_activate.cljs` demonstrates a robust pattern for disposable cleanup and management that can be handy for re-runnable scripts.

### Keyboard Shortcuts Integration
Scripts include keyboard binding definitions as comments:
```clojure
;; {
;;   "key": "ctrl+alt+j s",
;;   "command": "joyride.runCode",
;;   "args": "(next-slide/activate!)"
;; }
```

## Presentation System Architecture

### Slide Navigation (`next_slide.cljs`)
- Markdown-based slides with VS Code preview
- Keyboard navigation with `next-slide:active` context
- Zen mode integration for distraction-free presenting
- CSS styling via `next-slide.css`
- Clicker support

### Slide Navigation Examples

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

### Configuration System
- `slides.edn`: EDN configuration defining slide order (vector ordering is authoritative)
- Markdown files in `slides/` directory
- Relative path resolution from workspace root
- AI prompts and instructions for presentation system reference this configuration

### Audio Integration
- TTS generation using OpenAI API (requires API key)
- Webview-based audio playback (browser security model)
- Audio service initialization in workspace activation

### Complete Audio Workflow Examples

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

## Interactive Examples (`live_examples.cljs`)

Demonstrates core Joyride patterns:
- VS Code API usage (messages, commands, configuration)
- Event handling with proper cleanup
- Status bar item creation and animation
- Extension API integration (e.g., Calva)
- NPM module usage and ClojureScript interop

### Key Example Patterns
```clojure
;; Configuration updates
(.update (vscode/workspace.getConfiguration "editor")
         "fontSize" 20 vscode/ConfigurationTarget.Global)

;; Event handling with cleanup
(def disposable (vscode/workspace.onDidOpenTextDocument handler))
(.dispose disposable)

;; Extension API integration
(when-let [ext (vscode/extensions.getExtension "extension.id")]
  (when (.-isActive ext)
    (use-extension-api ext)))
```

## Development Environment Setup

### Prerequisites
1. **Joyride extension** installed
2. **Calva extension** recommended for REPL development
3. **Node dependencies**: `npm install` in project root

### REPL Development
1. Command: `Calva: Start Joyride REPL and Connect` (human workflow - AI agents use `joyride_evaluate_code` directly)
2. Evaluate expressions with `alt+enter` (top-level form) (human workflow)
3. Use `workspace_activate.cljs` as entry point for exploration
4. **Dual REPL Environment**: When both human and AI are connected, they share the same REPL environment for collaborative development

### Audio Features (Optional)
- Requires OpenAI API key in environment for TTS
- Audio service requires user interaction (Enable Audio button)

## Code Assistance Guidelines

### When Working with This Project
1. **Always use REPL first** - Validate with `joyride_evaluate_code` before editing
2. **Manage disposables properly** - Essential for re-runnable scripts
3. **Follow ClojureScript patterns** - Functional, data-oriented, with destructuring, and domain-namespaced keywords
4. **Test incrementally** - Build solutions step by step in the REPL
5. **Consult human intelligence** - Use `joyride_request_human_input` when uncertain or need domain knowledge
6. **Understand workspace activation** - Scripts may run automatically via `workspace_activate.cljs` (when relevant to the task)

### Common Operations
- **Run workspace script**: `Joyride: Run Workspace Script...` (human workflow - AI can test scripts)
- **Direct code execution**: `joyride.runCode` command
- **REPL exploration**: AI uses `joyride_evaluate_code`, humans connect via Calva

### File Organization
- **Scripts** (`.joyride/scripts/`): Runnable from Joyride menus
- **Source** (`.joyride/src/`): Library functions called by shortcuts/scripts
- **Workspace scope**: Project-specific automation
- **User scope**: Global automation (`~/.config/joyride/`)

## Usage Examples and Workflows

### REPL-First Development Pattern
Always validate code in the REPL before suggesting file changes:

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
```

### Audio System Usage
Generate speech and coordinate with slides:

```clojure
(in-ns 'user)
;; Generate audio for a slide
(require '[ai-presenter.audio-generation :as audio]
         '[next-slide :as slides]
         '[ai-presenter.audio-playback :as playback])

;; Create audio for current slide content
(audio/generate-speech-for-current-slide!)

;; Present a specific slide with audio
(slides/show-slide! "slides/welcome.md")

;; Quick audio generation and playback (requires OpenAI API key)
(audio/generate-and-play-message!+ "Hello Chat! This is generated speech.")

;; Load and play existing audio file
(playback/load-and-play-audio!+ "slides/voice/welcome.mp3")

;; Generate audio file for a slide (saves to slides/voice/)
(audio/generate-slide-audio!+ "welcome" "Welcome to the demonstration!")

;; Navigate slides programmatically
(slides/next! true)   ; next slide
(slides/next! false)  ; previous slide
(slides/restart!)     ; first slide
(slides/current!)     ; show current slide
```

### Live Coding Demo Patterns
Common patterns from `live_examples.cljs`:

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

### Timer/Showtime Widget Example
Create a live updating status bar timer:

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

### Workspace Automation Patterns
From `workspace_activate.cljs` - startup automation:

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

### Advanced File and UI Operations

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

### Human-AI Collaboration Protocol
When uncertain or exploring new territory:

1. **Use REPL to explore**: Start with small expressions to understand the system
2. **Validate assumptions**: Test API availability and behavior before committing
3. **Ask for guidance**: Use `joyride_request_human_input` for domain knowledge or when stuck
4. **Share findings**: Show your REPL exploration in code blocks with `(in-ns ...)` headers

Example collaboration flow:
```clojure
(in-ns 'user)
;; Explore what's available
(require '["vscode" :as vscode])
(keys (js->clj vscode/window))  ; What window methods exist?

;; Test specific functionality
(vscode/window.showInformationMessage "Testing..." "OK" "Cancel")
;; ^ This shows I need awaitResult: true to get the button choice
```

### Complete Demo Scenarios
**Coordinated Slide and Audio Presentation:**
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

**Live Coding with Animation Demo:**
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

**Extension Integration Demo:**
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

## Essential Validation Practices

### Before Any Code Suggestions
1. **Test in REPL**: Use `joyride_evaluate_code` to verify API calls work
2. **Check dependencies**: Ensure required extensions/modules are available
3. **Validate paths**: Test file system operations with actual workspace structure
4. **Ask when uncertain**: Use `joyride_request_human_input` for clarification

### REPL Reality Checks
The REPL is the ultimate source of truth. When in doubt:
- Test small expressions first
- Verify object properties and methods exist
- Check async operations behavior
- Confirm extension API availability

This collaborative approach ensures working solutions backed by actual testing rather than speculation.

This project serves as both a functional presentation system and a comprehensive example of Joyride's capabilities for workspace-specific VS Code automation.