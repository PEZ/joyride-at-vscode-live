# Joyride Live Demo Supporting Project

This is a **Joyride workspace automation project** demonstrating VS Code customization and presentation tooling using ClojureScript. The project showcases interactive programming with Joyride, where VS Code becomes scriptable in user space using the full VS Code Extension API. The project supports a streamed live demo of Joyride on the VS Code YouTube channel. Address the audience as "Chat".

## Essential Joyride Information Sources

**Always use these tools first** to get comprehensive, up-to-date information:

- `joyride_basics_for_agents` - Technical guide for LLM agents using Joyride evaluation capabilities
- `joyride_assisting_users_guide` - Complete user assistance guide with project structure, patterns, examples, and troubleshooting

These tools contain all the detailed information about Joyride APIs, project structure, common patterns, user workflows, and troubleshooting guidance.

## Project Architecture

### Core Components
- **Joyride Scripts**: Workspace-specific automation in `.joyride/` (version-controlled automation)
- **Presentation System**: Custom slideshow with navigation, speaker notes, and audio playback demonstrating Joyride capabilities
- **Interactive Examples**: Live coding demonstrations and REPL-driven development patterns in `live_examples.cljs`
- **Audio Tooling**: TTS generation and webview-based audio playback system for presentations
- **Timer Widget**: Status bar timer for presentation time management

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
│   ├── live_examples.cljs         # Interactive coding examples (1381 lines)
│   ├── next_slide.cljs           # Presentation navigation system
│   ├── next_slide_notes.cljs     # Speaker notes management
│   ├── showtime.cljs             # Timer status bar widget
│   └── ai_presenter/             # Audio generation & playback
│       ├── audio_generation.cljs # TTS generation using OpenAI API
│       ├── audio_playback.cljs   # Webview-based audio control
│       └── opening_sequence.cljs  # Automated presentation opener
└── resources/
    └── audio-service.html        # Webview for browser audio

slides/                           # Markdown presentation content
slides.edn                       # Slide deck configuration (authoritative ordering)
next-slide.css                   # Presentation styling
package.json                     # NPM dependencies (ai-text-to-speech, posthtml-parser)
```

## Development Patterns

### Interactive Programming Workflow
This project demonstrates **REPL-driven development**:
1. Use `joyride_evaluate_code` tool to test code before editing files
2. Develop incrementally - evaluate small expressions to build up solutions
3. Always verify API usage in the REPL before suggesting changes
4. Prefer pure functions over side effects

### Workspace Activation Pattern

The `workspace_activate.cljs` script demonstrates essential Joyride patterns:
- **Disposable Management**: Tracks and cleans up event handlers for script re-runnability
- **Extension Context Integration**: Registers disposables with VS Code's lifecycle
- **Automated Initialization**: Sets up presentation system, audio service, and status bar on workspace load
- **Cross-namespace Coordination**: Orchestrates `next-slide`, `showtime`, and `ai-presenter` modules

Critical pattern for reloadable scripts:
```clojure
(defonce !db (atom {:disposables []}))

(defn- clear-disposables! []
  (run! #(.dispose %) (:disposables @!db))
  (swap! !db assoc :disposables []))

(defn- push-disposable [disposable]
  (swap! !db update :disposables conj disposable)
  (-> (joyride/extension-context) .-subscriptions (.push disposable)))
```

### Disposable Management
Critical pattern for Joyride scripts and REPL experimentation - always track disposables to prevent resource leaks:

```clojure
;; Basic pattern: Use def to hold disposables for manual cleanup
(def my-disposable
  (vscode/workspace.onDidOpenTextDocument handler))

;; Clean up when done
(.dispose my-disposable)

;; For multiple disposables, collect in a vector
(def my-disposables
  [(vscode/workspace.onDidOpenTextDocument handler1)
   (vscode/commands.registerCommand "my.command" handler2)])

;; Clean up all
(run! #(.dispose %) my-disposables)

;; Always register with extension context to prevent leaks on deactivation
(-> (joyride/extension-context) .-subscriptions (.push my-disposable))
```

For systematic/framework approaches, see the `workspace_activate.cljs` pattern which demonstrates reloadable script architecture with automatic disposable tracking.

### Keyboard Shortcuts Integration
Scripts include keyboard binding definitions as comments for easy copy-paste:
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

### Configuration System
- `slides.edn`: EDN configuration defining slide order (vector ordering is authoritative)
- Markdown files in `slides/` directory
- Relative path resolution from workspace root
- AI prompts and instructions for presentation system reference this configuration

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

### Audio Integration
- TTS generation using OpenAI API (requires API key)
- Webview-based audio playback (browser security model)
- Audio service initialization in workspace activation

### Live Coding Demo Patterns
Key patterns from `live_examples.cljs` demonstrate:
- VS Code API usage (messages, commands, configuration)
- Event handling with proper cleanup
- Status bar item creation and animation
- Extension API integration (e.g., Calva)
- NPM module usage and ClojureScript interop

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

### ⚠️ Critical: awaitResult Usage

**Use `awaitResult: true` only when you are evaluating an async expression and when you need the return value!**

```clojure
;; Need return value → awaitResult: true
(slides/get-current-slide-name+)               ; Get slide name
(audio/generate-and-play-message!+ "Hello")     ; Wait for completion
(vscode/window.showInformationMessage "Test" "OK" "Cancel")   ; Get user choice

;; Side effects only → omit awaitResult (defaults to false)
(slides/activate!)                             ; Start system
(vscode/window.showInformationMessage "Hello")   ; Nothing to wait for
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