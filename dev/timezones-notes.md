# Timezone Converter Implementation Notes

## Core Implementation Plan

### REPL-Driven Development Philosophy

**Critical**: Follow interactive programming principles throughout implementation:

1. **Build incrementally** - Test each function in the REPL before adding to file
2. **Data-oriented approach** - Functions take args and return results, minimize side effects
3. **Step-by-step verification** - Evaluate each sub-expression to understand behavior
4. **Destructuring preferred with namespaced keywords** - Use `{:timezone/keys [label id]}` over manual property access for better refactoring support
5. **No println debugging** - Prefer evaluating sub-expressions in REPL for testing
6. **Domain-namespaced keywords** - Use `:timezone/id`, `:ui/state`, `:result/data` for better refactoring support and namespace clarity

**Implementation workflow:**
```clojure
(comment
  ;; Test current time pre-population
  (format-current-time-for-input)
  ;=> "2025-08-26 17:56"

  ;; Test user editing experience
  (vscode/window.showInputBox
    #js {:prompt "Edit date and time, or press Enter to use current time"
         :value (format-current-time-for-input)})
  ;=> User can easily edit just the parts they want to change

  ;; Test timezone conversion
  (def test-date (js/Date. "2025-08-26 15:30"))
  (format-for-timezone test-date "America/New_York")
  ;=> "Aug 26, 2025, 9:30 AM EDT"

  ;; Test with all timezones (namespaced keys)
  (map #(assoc % :timezone/formatted-time (format-for-timezone test-date (:timezone/id %))) timezones)

  ;; Test destructuring with domain namespaces
  (let [{:timezone/keys [label id]} (first timezones)]
    [label id])
  ;=> ["UTC" "UTC"]

  ;; Test quick pick creation
  (def qp (vscode/window.createQuickPick))
  ;; ... continue building up the solution step by step
  )
```

### Script Structure
- Single Joyride ClojureScript file: `.joyride/scripts/timezones.cljs`
- Direct script execution through Joyride
- Leverage JavaScript's `Intl.DateTimeFormat` for timezone conversion

**When ready to create and update code files**: Then also add any exploratory code verifying functions and functionality, as Rich Comment Forms (`(comment ...)` blocks) to preserve REPL discoveries in the script.

For new files and plain appending of content, the built in edit tools work best. Then if functions need to be edited, replace functions in full one at a time using the structural editing tools.

### Timezone Configuration (Domain-Namespaced Keys)
```clojure
(def timezones
  [{:timezone/label "UTC" :timezone/id "UTC"}
   {:timezone/label "New York (ET)" :timezone/id "America/New_York"}
   {:timezone/label "Los Angeles (PT)" :timezone/id "America/Los_Angeles"}
   {:timezone/label "London (GMT)" :timezone/id "Europe/London"}
   {:timezone/label "Berlin (CET)" :timezone/id "Europe/Berlin"}
   {:timezone/label "Tokyo (JST)" :timezone/id "Asia/Tokyo"}
   {:timezone/label "Sydney (AEST)" :timezone/id "Australia/Sydney"}])
```

### User Flow (REPL-Verified)
1. ✅ User runs the user script `timezones.cljs`
2. ✅ `vscode/window.showInputBox` prompts with **pre-populated current time** for easy editing:
   - `value`: Current time in "2025-08-26 17:56" format (REPL-tested)
   - `prompt`: "Edit date and time, or press Enter to use current time"
   - `placeHolder`: "YYYY-MM-DD HH:MM format"
3. ✅ Parse input using `parse-input` function (handles current time default as safety net)
4. ✅ Convert to each configured timezone using tested `format-for-timezone`
5. ✅ Present results in `vscode/window.createQuickPick` with global copy-all button
6. ✅ Handle selection → copy individual timezone to clipboard

### Quick Pick Items Structure (Namespaced Keys)
```clojure
{:label "New York (ET): Aug 26, 2025 3:30 PM"
 :detail "EDT (UTC-4)"
 :timezone-data {:timezone/id "America/New_York" :timezone/formatted-time "..."}}
```

### Copy to Clipboard Functions (REPL-Verified)
- ✅ Individual selection: Copy single timezone result via `vscode/env.clipboard.writeText`
- ✅ Global action: Use `vscode/window.createQuickPick` instead of `showQuickPick` to enable global buttons via the `buttons` property
- ✅ Each item can also have individual copy buttons via `QuickPickItem.buttons`
- ✅ Handle button events through `onDidTriggerButton` (global) and `onDidTriggerItemButton` (per-item)
- ✅ Markdown list generation tested and working for "copy all" functionality

### Error Handling (REPL-Tested)
- ✅ Invalid date input → `parse-input` returns `nil`, can show error message and re-prompt
- ✅ Timezone conversion errors → graceful degradation (tested with all major timezones)
- ✅ Empty input → uses current time as default (verified: `(js/Date.)` in `parse-input`)
- ✅ Escape key → abort operation (VS Code input box handles this natively)
- ✅ Menu maintains focus when user clicks elsewhere (QuickPick default behavior)

### Technical Implementation Details

#### Date Input Pre-population (REPL-Tested)
```clojure
(defn format-current-time-for-input []
  (let [now (js/Date.)
        year (.getFullYear now)
        month (inc (.getMonth now))  ; getMonth is 0-based
        day (.getDate now)
        hours (.getHours now)
        minutes (.getMinutes now)]
    (str year "-"
         (when (< month 10) "0") month "-"
         (when (< day 10) "0") day " "
         (when (< hours 10) "0") hours ":"
         (when (< minutes 10) "0") minutes)))
;; Tested: Returns "2025-08-26 17:56" - clean, editable format
```

#### Date Parsing Strategy (REPL-Tested)
```clojure
(defn parse-input [input]
  (if (or (nil? input) (= input ""))
    (js/Date.)  ; current time - verified in REPL
    (let [parsed (js/Date. input)]
      (if (js/isNaN (.getTime parsed))
        nil  ; invalid date - return nil for error handling
        parsed))))
```

#### Timezone Conversion (REPL-Verified)
```clojure
(defn format-for-timezone [date tz]
  (.toLocaleString date "en-US"
    #js {:timeZone tz
         :year "numeric"
         :month "short"
         :day "numeric"
         :hour "numeric"
         :minute "2-digit"
         :timeZoneName "short"}))
;; Tested: Returns "Aug 26, 2025, 9:30 AM EDT" for America/New_York
```

#### Quick Pick Item Creation (REPL-Tested with Namespaced Keys)
```clojure
(defn create-timezone-items [date timezone-configs]
  (map (fn [{:timezone/keys [label id]}]
         (let [formatted-time (format-for-timezone date id)]
           #js {:label (str label ": " formatted-time)
                :detail id
                :timezone-data #js {:timezone/id id :timezone/formatted-time formatted-time}}))
       timezone-configs))
```

#### Markdown List Generation (REPL-Verified)
```clojure
(defn create-markdown-list [timezone-items]
  (->> timezone-items
       (map #(str "- " (.-label %)))
       (clojure.string/join "\n")))
;; Produces: "- UTC: Aug 26, 2025, 1:30 PM UTC\n- New York (ET): ..."
```

#### VS Code Integration Points
- Input collection: `vscode.window.showInputBox`
- Result presentation: `vscode.window.createQuickPick` (enables global buttons)
- Button event handling: `onDidTriggerButton`, `onDidTriggerItemButton`
- Clipboard: `vscode.env.clipboard.writeText`

### File Organization
```
.joyride/
  scripts/
    timezones.cljs  # Main implementation
```

### Testing Strategy

#### Human-AI Collaboration for UI Widgets
Interactive widgets like `showInputBox` and `createQuickPick` require human interaction for proper testing:

1. **Tell human** - "About to show input dialog for testing"
2. **Evaluate in REPL** - Use `awaitResult: true` to wait for user interaction
3. **Human responds** - User interacts with the widget (types, selects, clicks)
4. **Confirm result** - Agent processes and confirms what was submitted

**Essential REPL pattern for UI testing:**
```clojure
;; Use awaitResult: true for user interactions
(vscode/window.showInputBox #js {:prompt "Test prompt"})
;; Agent waits, human responds, agent gets actual result

(vscode/window.showQuickPick #js ["Option 1" "Option 2"])
;; Agent waits, human selects, agent gets selection
```

**Before implementing**: Read `joyride_basics_for_agents` and `joyride_assisting_users_guide` tools to learn Joyride evaluation capabilities and user assistance patterns, then use human-AI collaboration testing to understand widget behavior.

#### REPL-Driven Development Testing
- **Interactive Development**: Test each function incrementally in the REPL before adding to file
- **Verified API Calls**: All VS Code API usage has been tested in the REPL:
  - ✅ `vscode/window.showInputBox` with placeholder and prompt
  - ✅ `vscode/window.createQuickPick` returns working QuickPick object
  - ✅ `vscode/env.clipboard.writeText` successfully copies to clipboard
  - ✅ `Intl.DateTimeFormat` via `toLocaleString` works perfectly for timezone conversion
- **Verified Date Parsing**: Tested with multiple input formats:
  - ✅ Empty input defaults to current time
  - ✅ "2025-08-26 15:30" parses correctly
  - ✅ "Aug 26, 2025 3:30 PM" parses correctly
  - ✅ Invalid dates return `nil` for error handling
- **Verified Data Transformation**: Confirmed timezone data structure and markdown generation
- Test with various date input formats across DST boundaries
- Validate error handling for edge cases

### Dependencies
- No external ClojureScript dependencies required
- Relies on VS Code API and Electron Date/Intl APIs
- Pure Joyride + SCI environment

### Future Enhancements (Out of Scope)
- Persistent storage of recent conversions
- Menu remembers last selected timezone option
- Integration with calendar applications
- Recurring meeting time suggestions
