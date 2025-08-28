# Joyrid- **Presentation System**: Custom slideshow ### Disposable Management
Critical pattern for Joyride scripts, especially when experimenting in the REPL:
```clojure
;; Always register event handlers with extension context to prevent leaks
(push-disposable
  (vscode/workspace.onDidOpenTextDocument handler))
```
The `workspace_activate.cljs` demonstrates a robust pattern for disposable cleanup and management that can be handy for re-runnable scripts.gation, notes, and audio playback (demo of Joyride capabilities, not the primary presentation format). Agents should understand audio generation for slides, ad-hoc speech, and coordinating `next-slide` with `audio-playback` namespaces Live Demo Project - Copilot Instructions

This is a **Joyride workspace automation project** demonstrating VS Code customization and presentation tooling using ClojureScript. The project showcases interactive programming with Joyride, where VS Code becomes scriptable in user space using the full VS Code Extension API. The project supports a streamed live demo of Joyride on the VS Code Youtube channel. Address the audience as “Chat”.

## Project Architecture

### Core Components
- **Joyride Scripts**: Workspace-specific automation in `.joyride/` (version-controlled automation)
- **Presentation System**: Custom slideshow with navigation, notes, and audio playback ;FEEDBACK: This presentation system is included as a demo of what Joyride can do, and not used for the presentation (which is demo, not a talk). This gets important when “thinking” about the project. It's important that the agent knows how to do things like generate audio for slides, generate audio for just saying something, and how to use the next-slide namespace together with the audio-plauback namespace to present a slide when/if asked to.
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
;FEEDBACK: This is Joyride and not “extensions”. Author this part as a reminder to keep track of disposables, both for scripts in general, but also extra important in the REPL, while experimenting. The pattern included here may be overkill. But mention that the activation script does have a pattern that can be handy.

Critical pattern for VS Code extensions:
```clojure
;; Always register event handlers with extension context
(push-disposable
  (vscode/workspace.onDidOpenTextDocument handler))
```

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

### Configuration System
- `slides.edn`: EDN configuration defining slide order (vector ordering is authoritative)
- Markdown files in `slides/` directory
- Relative path resolution from workspace root
- AI prompts and instructions for presentation system reference this configuration

### Audio Integration
- TTS generation using OpenAI API (requires API key)
- Webview-based audio playback (browser security model)
- Audio service initialization in workspace activation

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
1. Command: `Calva: Start Joyride REPL and Connect` ;FEEDBACK: this is for the human, not needed for the agent, which can use `joyride_evaluate_code` without starting any REPL. But good to include this, and point out that when both the human and the AI are connected they can co-op connected to the same system and repl-environment.
2. Evaluate expressions with `alt+enter` (top-level form) ;FEEDBACK: Again, for the human
3. Use `workspace_activate.cljs` as entry point for exploration

### Audio Features (Optional)
- Requires OpenAI API key in environment for TTS
- Audio service requires user interaction (Enable Audio button)
;FEEDBACK: This has already been mentioned. Please check the whole doc for redundancy removal wins.

## Code Assistance Guidelines

### When Working with This Project
1. **Always use REPL first** - Validate with `joyride_evaluate_code` before editing
2. **Understand workspace activation** - Scripts run automatically via `workspace_activate.cljs` ;FEEDBACK: This is important only sometimes. Maybe place last in the list and qualify it a bit.
3. **Manage disposables properly** - Essential for re-runnable scripts
4. **Follow ClojureScript patterns** - Functional, data-oriented, with destructuring, and domain-namespaced keywords
5. **Test incrementally** - Build solutions step by step in the REPL

### Common Operations
- **Run workspace script**: `Joyride: Run Workspace Script...` ;FEEDBACK: Human facing, even if the AI can also run scripts when testing them of course.
- **Evaluate selection**: `Joyride: Evaluate Selection` ;FEEDBACK: Human facing. Not important for the AI, I think.
- **Direct code execution**: `joyride.runCode` command
- **REPL exploration**: Connect Calva and evaluate expressions ;FEEDBACK: Human facing, see earlier note about the dual repl command.

### Extension Integration
Check extension availability before using APIs:
```clojure
(when-let [ext (vscode/extensions.getExtension "publisher.extension")]
  (when (.-isActive ext)
    ;; Safe to use extension API
    ))
```
;FEEDBACK: Maybe slightly different context. But this has been mentioned earlier. Consider only keeping one.

### File Organization
- **Scripts** (`.joyride/scripts/`): Runnable from Joyride menus
- **Source** (`.joyride/src/`): Library functions called by shortcuts/scripts
- **Workspace scope**: Project-specific automation
- **User scope**: Global automation (`~/.config/joyride/`)

This project serves as both a functional presentation system and a comprehensive example of Joyride's capabilities for workspace-specific VS Code automation.