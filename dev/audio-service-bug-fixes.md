# Audio Service Bug Fix Plan

## Progress Tracking

**⚠️ IMPORTANT: Complete each fix individually and have human test before proceeding to the next one.**

- [x] **Fix 0: CRITICAL - User Gesture Playback Failure** - Status: ✅ COMPLETED 2025-08-17
- [x] **Fix 1: Prevent Concurrent Audio Loads** - Status: ✅ COMPLETED 2025-08-18
- [x] **Fix 2: Fix Premature "Playing" Status in Webview** - Status: ✅ COMPLETED 2025-08-18
- [x] **Fix 3: Remove Race Condition in play-audio!+** - Status: ✅ COMPLETED 2025-08-18
- [ ] **Fix 4: Improve Resolver ID Validation** - Status: Not Started
- [ ] **Fix 5: Add Missing Audio Event Handlers in Webview** - Status: Not Started
- [ ] **Fix 6: Improve Error Messages in Timeout Handler** - Status: Not Started

## Overview
This plan addresses critical bugs in the audio service status reporting and timing issues. The fixes are ordered by dependency and impact.

## Development Methodology
**🔧 TEST-DRIVEN BUG FIXING**: Each fix starts by documenting the bug with a failing REPL test, then implementing the fix until the test passes.

### Required Tools
- **Joyride Evaluation Tool**: Use `joyride_evaluate_code` for all testing and experimentation
- **REPL-Driven Development**: Test each change immediately in the REPL before committing to files
- **Regression Test Documentation**: Document failing tests in `dev/testing-audio-service.md`

### Test-Driven Fix Workflow
1. **Expose the problem**: Create REPL session that demonstrates the bug
2. **Document the test**: Write the failing test in `dev/testing-audio-service.md`
3. **Implement the fix**: Edit files to resolve the issue
4. **Validate the fix**: Ensure the documented test now passes
5. **Update documentation**: Mark the test as "SHOULD PASS" when fixed

### Regression Test Document
The file `dev/testing-audio-service.md` will contain:
- **Bug demonstration tests**: REPL sessions that expose each issue
- **Expected vs actual behavior**: Clear documentation of what should happen
- **Post-fix validation**: Same tests that should pass after fixes
- **Regression prevention**: Future testing to ensure bugs don't return

### Interactive Programming Workflow
1. **Load the namespace**: `(require '[ai-presenter.audio-playback :as audio] :reload)`
2. **Expose the bug**: Create REPL test showing the problem
3. **Document the test**: Add to regression test document
4. **Make incremental changes**: Edit files, reload, test immediately
5. **Validate fixes**: Ensure documented test now passes

### Example REPL Session
```clojure
;; Always start by loading the audio namespace
(require '[ai-presenter.audio-playback :as audio] :reload)

;; FRESH START: Initialize audio service to clean state
(audio/dispose-audio-webview!)  ;; Clean up any existing webview
(audio/init-audio-service!)     ;; Create fresh webview

;; Check initial state - should show userGestureComplete: false
(audio/get-audio-status!+)
(audio/check-user-gesture!+)    ;; Should return false

;; Test load without user gesture - should fail gracefully
(audio/load-audio!+ "dev/test-resources/audio-play-test-very-short.mp3")
;; Should get: "Audio loaded but requires user gesture. Duration: 1.176s. Please click 'Enable Audio'."

;; Test load-and-play - should prompt user automatically
(audio/load-and-play-audio!+ "dev/test-resources/audio-play-test-two-sentences.mp3")
;; Should show VS Code dialog asking user to enable audio

;; After user clicks Enable Audio, test normal operations
(audio/get-audio-status!+)
(audio/play-audio!+)

;; After making file changes, reload and test again
(require '[ai-presenter.audio-playback :as audio] :reload)
```

**⚠️ CRITICAL**: Every code change should be tested in the Joyride REPL before being considered complete.

**🎛️ HUMAN INTERACTION NOTE**: Many tests require the human to click "Enable Audio" in the webview dialog. **Audio playback verification requires human ears** - the AI agent cannot determine if audio actually plays.

## Fix 0: CRITICAL - User Gesture Playback Failure ✅ COMPLETED

**Bug**: User would click "Enable Audio" but playback system still required additional user gesture, causing "play() can only be initiated by a user gesture" errors.

**Fix**: Fixed webview audio gesture detection and promise resolution logic.

**Result**: Audio plays correctly after user enables it once. Regression test exists in testing doc.

---

## Fix 1: Prevent Concurrent Audio Loads ✅ COMPLETED

**Bug**: Multiple concurrent `load-audio!+` operations created orphaned promises and resolver conflicts. First promise would never resolve/reject when a second load started.

**Fix**: Implemented single-load policy - new loads immediately cancel existing ones with clear rejection message. Refactored state from `{:load-resolvers {}}` to `{:current-load-resolver nil}` to make the constraint explicit.

**Result**: Only one load operation allowed at any time. Clean promise handling with no memory leaks.

---

## Fix 2: Fix Premature "Playing" Status in Webview ✅ COMPLETED

**Bug**: Webview set `playbackState = 'playing'` before `audio.play()` was called, causing immediate false "playing" status reports.

**Fix**: Moved status update inside `audio.play().then()` success handler so status only shows "playing" after audio actually starts.

**Result**: Status reports are now accurate - "playing" only appears when audio is genuinely playing.

---

## Fix 3: Remove Race Condition in play-audio!+
**File**: `audio_playback.cljs`
**Function**: `play-audio!+`
**Priority**: High (causes inaccurate status reporting)

### Issue
Second status request happens immediately after play command, before audio actually starts.

### Solution
Remove the second status fetch and rely on the pre-play status check only.

```clojure
;; BEFORE:
(if (:ready? readiness)
  (do
    (send-audio-command! :play {:id (or id "default")})
    ;; Get updated status after play command
    (p/let [new-status (get-audio-status!+)]
      {:success true
       :action :played
       :readiness readiness
       :status-before status
       :status-after new-status}))

;; AFTER:
## Fix 3: Remove Race Condition in play-audio!+ ✅
**File**: `audio_playback.cljs`
**Function**: `play-audio!+`
**Priority**: High (causes inaccurate status reporting)

### Issue
Second status request happens immediately after play command, before audio actually starts.

### Solution
Remove the second status fetch and rely on the pre-play status check only.

**Result**: Eliminated race condition - no more immediate status fetch after play command.
```

### Files to Edit
- `/Users/pez/Projects/Meetup/joyride-at-vscode-live/.joyride/src/ai_presenter/audio_playback.cljs` (lines ~187-203)

### Testing After Fix 3
```clojure
;; play-audio!+ should return immediately without :status-after
(def result (audio/play-audio!+))
;; Should not contain :status-after key
(keys result)
```

**🛑 STOP: Have human verify Fix 3 works before proceeding to Fix 4**

---

## Fix 4: Improve Resolver ID Validation
**File**: `audio_playback.cljs`
**Function**: Message handler in `init-audio-service!`
**Priority**: Medium (prevents silent bugs)

### Issue
Fallback "any resolver" logic masks ID mismatches and could resolve wrong promises.

### Solution
Add strict validation and clear error logging for ID mismatches.

```clojure
;; Replace the fallback logic with strict validation:
(if-let [resolver-map (get-in current-state [:load-resolvers audio-id])]
  (do
    ((:resolve resolver-map) load-data)
    (swap! !state remove-resolver :load-resolvers audio-id))
  ;; Log error instead of silent fallback
  (do
    (println "❌ No resolver found for audio-id:" audio-id "Available:" (keys (:load-resolvers current-state)))
    (vscode/window.showWarningMessage (str "Audio ready notification for unknown ID: " audio-id))))
```

### Files to Edit
- `/Users/pez/Projects/Meetup/joyride-at-vscode-live/.joyride/src/ai_presenter/audio_playback.cljs` (lines ~140-150)
- Apply same pattern to error handler (lines ~150-165)

### Testing After Fix 4
```clojure
;; Test with mismatched ID - should show clear error
(audio/load-audio!+ "dev/test-resources/audio-play-test-very-short.mp3" :id "test-id")
;; Manually trigger webview message with different ID to see validation
```

**🛑 STOP: Have human verify Fix 4 works before proceeding to Fix 5**

---

## Fix 5: Add Missing Audio Event Handlers in Webview
**File**: `audio-service.html`
**Priority**: Medium (improves status accuracy)

### Issue
Missing handlers for `waiting`, `stalled`, `suspend` events leads to incomplete status tracking.

### Solution
Add comprehensive event listeners for better state tracking.

```javascript
// Add after existing event listeners:
audio.addEventListener('waiting', () => {
    log('Audio: waiting - loading more data');
    audioStatus.playbackState = 'loading';
});

audio.addEventListener('stalled', () => {
    log('Audio: stalled - network issues');
    audioStatus.playbackState = 'stalled';
});

audio.addEventListener('suspend', () => {
    log('Audio: suspend - loading suspended');
    audioStatus.playbackState = 'suspended';
});
```

### Files to Edit
- `/Users/pez/Projects/Meetup/joyride-at-vscode-live/.joyride/resources/audio-service.html` (lines ~250-260)

### Testing After Fix 5
```clojure
;; Load a large audio file or simulate network issues to trigger new events
;; Check webview logs for new event types
```

**🛑 STOP: Have human verify Fix 5 works before proceeding to Fix 6**

---

## Fix 6: Improve Error Messages in Timeout Handler
**File**: `audio_playback.cljs`
**Function**: `load-audio!+` timeout logic
**Priority**: Low (developer experience)

### Issue
Timeout errors don't clearly distinguish between different failure modes and don't clearly indicate it's a timeout.

### Solution
Enhance error messages with more specific timeout information.

```clojure
(reject (js/Error.
         (case (:playbackState final-status)
           "loading" (str "Audio load timeout (still loading after " timeout-ms "ms): " local-file-path)
           "error" (str "Audio load timeout - failed with error: " (:lastError final-status))
           (str "Audio load timeout (state: " (:playbackState final-status) " after " timeout-ms "ms): " local-file-path))))
```

### Files to Edit
- `/Users/pez/Projects/Meetup/joyride-at-vscode-live/.joyride/src/ai_presenter/audio_playback.cljs` (lines ~240-245)

### Testing After Fix 6
```clojure
;; Test with invalid file to trigger timeout
(audio/load-audio!+ "nonexistent-file.mp3" :timeout-ms 1000)
;; Should get clear timeout message
```

**🛑 STOP: Have human verify Fix 6 works - ALL FIXES COMPLETE**

---

## Execution Order
1. **Fix 0** (CRITICAL - User gesture playback) - **MUST BE DONE FIRST** - Core functionality broken
2. **Fix 1** (Concurrent loads) - Must be done early to prevent new bugs during testing
3. **Fix 2** (Webview playing status) - Core bug fix
4. **Fix 3** (Race condition) - Depends on Fix 2 working correctly
5. **Fix 4** (ID validation) - Can be done in parallel with others
6. **Fix 5** (Event handlers) - Enhancement, can be done last
7. **Fix 6** (Error messages) - Enhancement, can be done last

## Testing Strategy
**🔬 JOYRIDE REPL-DRIVEN TESTING**: All testing must be done through the Joyride evaluation tool.

### Core Testing Workflow
After each fix:
1. **Initialize fresh state**: `(audio/dispose-audio-webview!)` then `(audio/init-audio-service!)`
2. **Reload namespace**: `(require '[ai-presenter.audio-playback :as audio] :reload)`
3. **Test with fresh webview**: `(audio/get-audio-status!+)` should show `userGestureComplete: false`
4. **Test basic functionality**: `(audio/load-and-play-audio!+ "dev/test-resources/audio-play-test-two-sentences.mp3")` (requires human to click Enable Audio)
5. **Check immediate status**: `(audio/get-audio-status!+)`
6. **Verify final state**: Use `js/setTimeout` with promise to check status after delay
7. **Test error cases**: Invalid file paths, concurrent operations

### Interactive Testing Examples
```clojure
;; FRESH START - Initialize to clean state
(audio/dispose-audio-webview!)
(audio/init-audio-service!)

;; Verify fresh state
(audio/get-audio-status!+)  ;; Should show userGestureComplete: false
@audio/!state

;; Test audio functionality
(audio/get-audio-status!+)
(audio/play-audio!+)

;; Test with delay to see final state (demonstrates the bug!)
(js/Promise.
 (fn [resolve reject]
   (js/setTimeout
     #(-> (audio/get-audio-status!+)
          (.then resolve)
          (.catch reject))
     2000)))

;; Test error cases - should throw immediately for invalid paths
(try
  (audio/load-audio!+ "nonexistent-file.mp3")
  (catch js/Error e
    (str "Error caught: " (.-message e))))
```

### REPL Validation Points
- Use `joyride_evaluate_code` with `awaitResult: true` for async operations
- Examine `@audio/!state` after each operation to understand internal state
- Test both success and failure paths for each function
- Verify promise resolution/rejection behavior

## Validation Criteria
- ✅ Status reports "stopped" when audio isn't playing
- ✅ Status reports "playing" only when audio is actually playing
- ✅ Concurrent loads properly reject previous operations
- ✅ Clear error messages for all failure modes
- ✅ No orphaned promises or memory leaks
- ✅ **All regression tests in `dev/testing-audio-service.md` pass**

## Final Notes
- **TEST-DRIVEN APPROACH REQUIRED**: Each fix starts by exposing the bug with REPL tests
- **Regression Testing**: Document all failing tests in `dev/testing-audio-service.md`
- **INTERACTIVE PROGRAMMING REQUIRED**: Use Joyride evaluation tool for all development
- Each fix should be implemented individually with REPL testing
- Human testing and approval required before proceeding to next fix
- Use `(require '[ai-presenter.audio-playback :as audio] :reload)` after each file change
- Examine `@audio/!state` frequently to understand system behavior
- Test both success and error paths in the REPL before marking fixes complete
- **Final deliverable**: Robust audio service + comprehensive regression test suite
- Update this document's progress checkboxes as fixes are completed
