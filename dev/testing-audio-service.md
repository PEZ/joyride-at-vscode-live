# Audio Service Regression Tests

This document contains REPL test sessions that expose bugs in the audio service. Each test should be run in the Joyride REPL to verify the audio service behaves correctly.

**⚠️ AI AGENT LIMITATIONS**:
- **Human interaction required**: AI cannot click "Enable Audio" button - human must do this
- **Audio playback verification**: AI cannot hear audio - human ears required to validate actual playback
- **User gesture dependency**: Many tests require human to complete browser security requirements

## How to Use This Document

1. **Run tests to expose bugs**: Execute each test session to see current (buggy) behavior
2. **Fix the code**: Implement fixes based on the bug plan
3. **Validate fixes**: Re-run the same tests to ensure they now pass
4. **Regression testing**: Use this document to ensure bugs don't reappear
5. **Human verification**: Agent must ask human to verify audio playback and user interactions

## Setup for All Tests

```clojure
;; Load audio namespace
(require '[ai-presenter.audio-playback :as audio] :reload)

;; Initialize fresh audio service state
(audio/dispose-audio-webview!)
(audio/init-audio-service!)

;; Verify fresh state
(audio/get-audio-status!+)
;; Should show: {:userGestureComplete false, :audioLoaded false, ...}
```

**🎛️ HUMAN INTERACTION NOTE**: Many tests require the human to click "Enable Audio" in the webview dialog. **Audio playback verification requires human ears** - the AI agent cannot determine if audio actually plays.

---

## Test 1: Concurrent Audio Load Handling

**Status**: 🔴 CURRENTLY FAILS - Multiple concurrent loads create resolver conflicts

**Test Session**:
```clojure
;; Setup fresh state
(audio/dispose-audio-webview!)
(audio/init-audio-service!)

;; Start two concurrent loads WITHOUT user gesture (to test resolver conflicts)
(def load1-promise (audio/load-audio!+ "slides/voice/demo-tts.mp3" :id "first"))
(def load2-promise (audio/load-audio!+ "slides/voice/demo-tts.mp3" :id "second"))

;; Check internal state for resolver conflicts
@audio/!state
;; ACTUAL BUG OBSERVED: Shows only {"first": {...}} - "second" resolver disappeared!
;; This means second load overwrote first without rejecting it properly
;; EXPECTED AFTER FIX: Only {"second": {...}} with first promise rejected

;; 🎛️ HUMAN: Click "Enable Audio" button in webview to complete the test
;; Then check what happens to the promises:
;; BUG: Both may resolve or wrong resolver used
;; EXPECTED: load1-promise rejects with "Load cancelled", load2-promise succeeds
```

**Expected Behavior (after fix)**:
- ✅ `load1-promise` should reject with "Load cancelled by new load operation"
- ✅ `load2-promise` should succeed
- ✅ `@audio/!state` should show only one resolver for "second" id
- ✅ No orphaned promises or memory leaks

**Current Behavior (bug)**:
- ❌ Both promises may succeed
- ❌ Resolver conflicts in state
- ❌ Potential memory leaks from orphaned resolvers

---

## Test 2: Audio Playing Status Accuracy

**Status**: 🔴 CURRENTLY FAILS - Reports "playing" immediately, not when actually playing

**Test Session**:
```clojure
;; Setup fresh state and load audio
(audio/dispose-audio-webview!)
(audio/init-audio-service!)

;; 🎛️ HUMAN: This will prompt you to click "Enable Audio" - do so to proceed
(audio/load-and-play-audio!+ "slides/voice/demo-tts.mp3")

;; Test immediate status after play command
(def play-result (audio/play-audio!+))
(get-in play-result [:status-after :playbackState])
;; BUG: Shows "playing" immediately, before audio actually starts

;; Check actual status after delay
(js/Promise.
 (fn [resolve reject]
   (js/setTimeout
     #(-> (audio/get-audio-status!+)
          (.then resolve)
          (.catch reject))
     2000)))
;; May show "stopped" indicating audio finished - proving status was wrong

;; 🎛️ HUMAN VERIFICATION REQUIRED:
;; Did you actually hear audio playing when status reported "playing"?
;; This test requires human ears to validate audio playback timing.
```

**Expected Behavior (after fix)**:
- ✅ `play-audio!+` should not report "playing" until audio actually starts
- ✅ Status should accurately reflect actual audio element state
- ✅ **Human can confirm audio timing matches status reports**
- ✅ No race conditions between command and status

**Current Behavior (bug)**:
- ❌ Reports `playbackState: "playing"` immediately
- ❌ May show "playing" when audio has already finished
- ❌ Race condition between play command and status check

---

## Test 3: Promise Race Conditions in play-audio!+

**Status**: 🔴 CURRENTLY FAILS - Second status request creates race condition

**Test Session**:
```clojure
;; Setup and load audio
(audio/dispose-audio-webview!)
(audio/init-audio-service!)
(audio/load-and-play-audio!+ "slides/voice/demo-tts.mp3")

;; Examine play-audio!+ return structure
(def result (audio/play-audio!+))
(keys result)
;; BUG: Contains both :status-before and :status-after

;; The :status-after is fetched too quickly after sending play command
```

**Expected Behavior (after fix)**:
- ✅ `play-audio!+` should not fetch status immediately after play command
- ✅ Should rely on webview events for status updates
- ✅ No race conditions in status fetching

**Current Behavior (bug)**:
- ❌ Fetches status immediately after sending play command
- ❌ Status-after may not reflect actual audio state
- ❌ Race condition between command dispatch and status fetch

---

## Test 4: Resolver ID Validation

**Status**: 🔴 CURRENTLY FAILS - Silent fallback masks ID mismatches

**Test Session**:
```clojure
;; This test requires manual manipulation of webview messages
;; Setup state with resolver
(audio/dispose-audio-webview!)
(audio/init-audio-service!)

;; Start load with specific ID
(def test-load (audio/load-audio!+ "slides/voice/demo-tts.mp3" :id "test-123"))

;; Check resolver state
(get-in @audio/!state [:load-resolvers])
;; Should show resolver for "test-123"

;; Manually trigger webview message with wrong ID would show fallback behavior
;; BUG: System silently uses "any resolver" instead of strict ID matching
```

**Expected Behavior (after fix)**:
- ✅ Strict ID validation for resolver matching
- ✅ Clear error logging when ID mismatch occurs
- ✅ No silent fallback to "any resolver"

**Current Behavior (bug)**:
- ❌ Falls back to any available resolver when ID doesn't match
- ❌ Silent masking of ID mismatches
- ❌ Could resolve wrong promises

---

## Test 5: Missing Audio Event Handling

**Status**: 🔴 CURRENTLY FAILS - Missing handlers for some audio events

**Test Session**:
```clojure
;; This test requires observing webview logs for missing events
;; Setup audio service and monitor events
(audio/dispose-audio-webview!)
(audio/init-audio-service!)

;; Load audio and observe which events are handled
(audio/load-audio!+ "slides/voice/demo-tts.mp3")

;; Check webview logs for event coverage
;; BUG: Missing handlers for 'waiting', 'stalled', 'suspend' events
```

**Expected Behavior (after fix)**:
- ✅ Comprehensive event handlers for all relevant audio events
- ✅ Status updates for waiting, stalled, suspend states
- ✅ Better status accuracy during network issues

**Current Behavior (bug)**:
- ❌ Missing event handlers for some audio states
- ❌ Incomplete status tracking
- ❌ Potential status inconsistencies during network issues

---

## Test 6: Timeout Error Messages

**Status**: 🔴 CURRENTLY FAILS - Unclear timeout error messages

**Test Session**:
```clojure
;; Test timeout with invalid file
(audio/dispose-audio-webview!)
(audio/init-audio-service!)

;; Try to load non-existent file with short timeout
(js/Promise.
 (fn [resolve reject]
   (-> (audio/load-audio!+ "nonexistent.mp3" :timeout-ms 1000)
       (.then #(resolve (str "Unexpected success: " %)))
       (.catch #(resolve (str "Error message: " (.-message %)))))))
```

**Expected Behavior (after fix)**:
- ✅ Clear timeout indication in error messages
- ✅ Specific information about timeout duration
- ✅ Different messages for different failure modes

**Current Behavior (bug)**:
- ❌ Error messages don't clearly indicate timeout
- ❌ Ambiguous between timeout and other failures
- ❌ Missing timeout duration in error messages

---

## Running All Tests

Execute this to run a comprehensive test suite:

```clojure
;; Comprehensive test runner - ⚠️ REQUIRES HUMAN INTERACTION
(defn run-audio-regression-tests []
  (println "🧪 Running Audio Service Regression Tests...")
  (println "⚠️  HUMAN: You will need to click 'Enable Audio' for several tests")
  (println "👂 HUMAN: You will need to verify actual audio playback with your ears")

  ;; Test 1: Concurrent loads
  (println "\n📋 Test 1: Concurrent Load Handling")
  ;; ... run test 1 code ...

  ;; Test 2: Status accuracy
  (println "\n📋 Test 2: Playing Status Accuracy - REQUIRES HUMAN EARS")
  ;; ... run test 2 code ...

  ;; Continue for all tests...

  (println "\n✅ All regression tests completed"))

;; Run the full suite
(run-audio-regression-tests)
```

**After all fixes are complete, all tests in this document should pass without errors.**

**🎛️ TESTING WORKFLOW FOR AI AGENTS**:
1. **AI runs REPL tests** to expose bugs and check internal state
2. **AI asks human** to perform required user interactions (clicking buttons)
3. **AI asks human** to verify audio playback behavior with their ears
4. **AI validates** promise resolution, state changes, and error messages
5. **Human confirms** the user experience is correct
