# Audio Service Regression Tests

This document contains REPL test sessions that expose bugs in the audio service. Each test should be run in the Joyride REPL to verify the audio service behaves correctly.

**⚠️ AI AGENT & HUMAN COLLABORATION REQUIREMENTS**:
- **Browser audio gesture requirement**: Audio service requires user gesture to enable sound playback (browser security)
- **Human gesture coordination**: AI initializes service, then requests human to click "Enable Audio" in webview using Joyride prompts
- **Audio playback confirmation**: When AI tests actual playback, human must confirm sound is heard
- **Collaborative flow**: AI runs all REPL commands and coordinates human actions through Joyride messaging

**🤝 COLLABORATION WORKFLOW**:
1. AI executes audio service initialization
2. AI uses Joyride to request human click "Enable Audio" button in webview
3. Human clicks button and confirms via Joyride response
4. AI continues with automated testing
5. When AI tests playback, AI asks human to confirm audio is heard
6. AI analyzes all test results and identifies bugs

## How to Use This Document

1. **Start testing session**: Run Session Initialization (requires one-time human gesture)
2. **Run core tests**: Execute all other tests relying on the initialized service
3. **End testing session**: Run Session Reset test to verify clean shutdown
4. **Fix bugs**: Implement fixes based on test results
5. **Re-run session**: Complete testing cycle to verify fixes work

## 🎯 TESTING SESSION STRUCTURE

**Key Insight**: Instead of requiring human interaction throughout testing, we structure the session to frontload the human requirements, then run uninterrupted automated tests.

### Session Flow:
1. **🎛️ Session Initialization** (Human interaction required once)
2. **🤖 Automated Test Suite** (No human interaction needed)
3. **🔄 Session Reset** (Verify clean shutdown for next session)

---

## 🎛️ SESSION INITIALIZATION (Human Required)

**Purpose**: Establish a fully functional audio service with user gesture completed, then validate it works end-to-end. All subsequent tests rely on this initialized state.

**🔍 INCLUDES CRITICAL REGRESSION TEST**: This initialization includes validation that Fix 0 (Critical User Gesture Bug) remains working - ensures audio playback succeeds after user gesture without the "play() can only be initiated by a user gesture" error.

```clojure
;; Load audio namespace
(require '[ai-presenter.audio-playback :as audio] :reload)

;; Start fresh
(audio/dispose-audio-webview!)
(audio/init-audio-service!)

;; Load test audio - this will prompt for user gesture
(audio/load-audio!+ "dev/test-resources/audio-play-test-two-sentences.mp3")
;; 🎛️ HUMAN: Ask human to click "Enable Audio" button in webview dialog when prompted

;; Verify user gesture was completed
(audio/check-user-gesture!+)
;; EXPECTED: true

;; Test actual playback to verify everything works
(audio/play-audio!+)
;; 👂 HUMAN: Confirm you hear audio playback (6.984 seconds of speech)
;; This validates the entire audio pipeline works end-to-end

;; 🔍 REGRESSION TEST: Critical User Gesture Bug (Fix 0)
;; This test validates that Fix 0 remains working - playback succeeds after user gesture
;; without the critical "play() can only be initiated by a user gesture" error
(def playback-result (audio/play-audio!+))
;; Validate that no user gesture error occurred
(def final-status (audio/get-audio-status!+))
;; CRITICAL ASSERTIONS (Fix 0 regression test):
;; ✅ play-audio!+ should return success: true
;; ✅ lastError should be nil (not user gesture error)
;; ✅ playbackState should be "playing" or "stopped" (not error state)
;; ❌ REGRESSION if lastError contains "play() can only be initiated by a user gesture"

;; Verify service is fully ready for subsequent tests
(audio/get-audio-status!+)
;; EXPECTED: {:userGestureComplete true, :audioLoaded true, :audioDataReady true, :lastError nil, ...}
```

**� SESSION READY**: Audio service is now fully initialized with user gesture complete. All subsequent tests can run without human interaction.

---

## 🤖 AUTOMATED TEST SUITE (No Human Interaction)

**Note**: These tests rely on the audio service being initialized from Session Initialization above. They can be run sequentially without human intervention.

### Tests That Should PASS ✅ (Baseline Functionality)

These tests verify functionality that currently works correctly and should continue to work after bug fixes.

#### Test A: Audio File Duration Detection

**Status**: ✅ SHOULD PASS - Duration detection works correctly
**Dependencies**: Uses initialized session (user gesture already complete)

**Test Session**:
```clojure
;; NOTE: Assumes Session Initialization was completed (user gesture available)
;; Test very short file duration detection with existing gesture
(js/Promise.
 (fn [resolve reject]
   (-> (audio/load-audio!+ "dev/test-resources/audio-play-test-very-short.mp3")
       (.then #(resolve (str "Very short file loaded: " %)))
       (.catch #(resolve (str "Very short error: " (.-message %)))))))
;; EXPECTED: Success with file loaded, or clear error message with duration

;; Test two-sentences file duration
(js/Promise.
 (fn [resolve reject]
   (-> (audio/load-audio!+ "dev/test-resources/audio-play-test-two-sentences.mp3")
       (.then #(resolve (str "Two sentences file loaded: " %)))
       (.catch #(resolve (str "Two sentences error: " (.-message %)))))))
;; EXPECTED: Success with file loaded, or clear error message with duration

;; Check that audio service preserves duration information
(audio/get-audio-status!+)
;; EXPECTED: Status should show current loaded audio file information
```

**Expected Behavior**:
- ✅ Audio file duration is correctly detected during load attempts
- ✅ With user gesture complete, files should load successfully
- ✅ Duration information is preserved in service state
- ✅ Duration detection works consistently within session#### Test B: Fresh Webview State Initialization

**Status**: ✅ SHOULD PASS - Clean initialization works correctly
**Dependencies**: Tests fresh initialization capability (separate from session)

**Test Session**:
```clojure
;; This test specifically validates initialization behavior
;; Save current state for restoration
(def saved-state @audio/!state)

;; Test fresh initialization in isolation
(audio/dispose-audio-webview!)
(audio/init-audio-service!)

;; Check initial state
(audio/get-audio-status!+)
;; EXPECTED: {:userGestureComplete false, :audioLoaded false, :audioDataReady false, :audioSrc nil, :playbackState "loading"}

;; Verify user gesture checking
(audio/check-user-gesture!+)
;; EXPECTED: false

;; NOTE: This test doesn't restore the session - Test C will re-establish if needed
```

**Expected Behavior**:
- ✅ Fresh webview starts with correct initial state
- ✅ `userGestureComplete` is false initially
- ✅ No audio is loaded initially
- ✅ Status requests work immediately after initialization

#### Test C: File Existence Validation

**Status**: ✅ SHOULD PASS - File validation works correctly
**Dependencies**: Uses initialized session

**Test Session**:
```clojure
;; NOTE: Using initialized session state
;; Test invalid file path - should fail immediately with clear error
(try
  (audio/load-audio!+ "nonexistent-file.mp3")
  "Should not reach here"
  (catch js/Error e
    (str "Caught error: " (.-message e))))
;; EXPECTED: Immediate error about file not existing

;; Test with absolute invalid path
(try
  (audio/load-audio!+ "/definitely/not/a/real/path.mp3")
  "Should not reach here"
  (catch js/Error e
    (str "Caught error: " (.-message e))))
;; EXPECTED: Immediate error about file not existing

;; Verify that audio service state isn't corrupted by invalid file attempts
(audio/get-audio-status!+)
;; EXPECTED: Service should still be in valid state, not corrupted by bad file attempts
```

**Expected Behavior**:
- ✅ Invalid file paths throw errors immediately (no async waiting)
- ✅ Clear error messages indicating file doesn't exist
- ✅ No webview state corruption from invalid file attempts
- ✅ Audio service remains functional after invalid file attempts

#### Test D: Additional Audio Operations (Using Initialized Service)

**Status**: ✅ SHOULD PASS - Additional operations work with initialized service
**Dependencies**: Relies on session initialization having completed user gesture

**Test Session**:
```clojure
;; NOTE: This assumes Session Initialization was completed
;; Verify service is ready
(audio/check-user-gesture!+)
;; EXPECTED: true (from Session Initialization)

;; Test loading different audio file with gesture already complete
(audio/load-audio!+ "dev/test-resources/audio-play-test-very-short.mp3")
;; EXPECTED: Should succeed without user gesture prompt

;; Test status after successful load
(audio/get-audio-status!+)
;; EXPECTED: {:userGestureComplete true, :audioLoaded true, :audioDataReady true, :audioSrc "...very-short.mp3"}

;; Test basic playback command (doesn't require listening)
(audio/play-audio!+)
;; EXPECTED: Returns status information showing playback initiated
```

**Expected Behavior**:
- ✅ Loading new audio files works without additional user gestures
- ✅ Service maintains user gesture completion across file loads
- ✅ Status reporting works consistently
- ✅ Play commands execute without errors

### Tests That Currently FAIL 🔴 (Bugs to Fix)

**Note**: These tests rely on specific initialization states as documented. Most use the initialized session, some require fresh state to test specific bugs.

#### Test 1: Single Load Operation Policy (Concurrent Load Rejection)

**Status**: ✅ SHOULD PASS - Fix 1 implemented: Prevents concurrent loads, properly rejects previous operations
**Dependencies**: Requires fresh webview state to test resolver conflicts cleanly
**Core Principle**: Audio service should only handle ONE load operation at a time

**Expected Behavior**:
1. **Only one active load**: Service maintains at most one pending load operation
2. **Immediate cancellation**: New load immediately cancels any existing pending load
3. **Clear rejection**: Cancelled load promises reject with "Load cancelled by new load operation"
4. **Clean state**: State shows only the most recent load operation

**Test Session**:
```clojure
;; This test specifically needs fresh state to expose resolver conflicts
(audio/dispose-audio-webview!)
(audio/init-audio-service!)

;; Start first load
(def load1-promise (audio/load-audio!+ "dev/test-resources/audio-play-test-very-short.mp3" :id "first"))
;; Immediately start second load (should cancel first)
(def load2-promise (audio/load-audio!+ "dev/test-resources/audio-play-test-two-sentences.mp3" :id "second"))

;; Check internal state - should show only the most recent load
@audio/!state
;; EXPECTED AFTER FIX: Only {"second": {...}} resolver exists
;; ACTUAL BUG: Both resolvers exist briefly, then "second" disappears, leaving only "first"

;; Test promise behavior - first should be rejected, second should be active
(js/Promise.
 (fn [resolve reject]
   (-> load1-promise
       (.then #(resolve (str "Load1 unexpected success: " %)))
       (.catch #(resolve (str "Load1 expected rejection: " (.-message %)))))))
;; EXPECTED AFTER FIX: "Load1 expected rejection: Load cancelled by new load operation"
;; ACTUAL BUG: Promise never resolves or rejects (orphaned)

(js/Promise.
 (fn [resolve reject]
   (-> load2-promise
       (.then #(resolve (str "Load2 unexpected success: " %)))
       (.catch #(resolve (str "Load2 error: " (.-message %)))))))
;; EXPECTED: "Load2 error: Audio loaded but requires user gesture. Duration: 6.984s"
```

**Expected Behavior (after fix)**:
- ✅ `load1-promise` should reject immediately with "Load cancelled by new load operation"
- ✅ `load2-promise` should become the active operation, waiting for user gesture
- ✅ `@audio/!state` should show only one resolver for "second" id
- ✅ No orphaned promises or memory leaks

**Current Behavior (FIXED in Fix 1)**:
- ✅ Only one resolver exists (single-load principle enforced)
- ✅ First promise properly rejected with clear message
- ✅ State is consistent (only most recent load operation tracked)
- ✅ No orphaned promises or memory leaks

**Previous Behavior (before fix)**:
- ❌ Both resolvers exist simultaneously (violates single-load principle)
- ❌ First promise never resolves or rejects (orphaned promise)
- ❌ State inconsistency (resolver disappears without proper cleanup)
- ❌ Memory leaks from unresolved promises#### Test 2: Audio Playing Status Accuracy

**Status**: 🔴 CURRENTLY FAILS - Reports "playing" immediately, not when actually playing
**Dependencies**: Relies on session initialization (user gesture complete)

**Test Session**:
```clojure
;; NOTE: Assumes Session Initialization completed user gesture
;; Load audio for testing (should succeed without gesture prompt)
(audio/load-audio!+ "dev/test-resources/audio-play-test-two-sentences.mp3")

;; Test immediate status after play command (correct async approach)
(p/let [play-result (audio/play-audio!+)]
  (get-in play-result [:status-after :playbackState]))
;; BUG: Shows "playing" immediately, before audio actually starts

;; Check actual status after brief delay to see if it was accurate
(js/Promise.
 (fn [resolve reject]
   (js/setTimeout
     #(-> (audio/get-audio-status!+)
          (.then (fn [status] (resolve (str "Status after delay: " (:playbackState status)))))
          (.catch reject))
     1000)))
;; May show "stopped" or other state, proving initial "playing" was wrong
```

**Expected Behavior (after fix)**:
- ✅ `play-audio!+` should not report "playing" until audio actually starts
- ✅ Status should accurately reflect actual audio element state
- ✅ No race conditions between command and status

**Current Behavior (bug)**:
- ❌ Reports `playbackState: "playing"` immediately without waiting
- ❌ May show "playing" when audio has already finished
- ❌ Race condition between play command and status check

#### Test 3: Promise Race Conditions in play-audio!+

**Status**: 🔴 CURRENTLY FAILS - Second status request creates race condition
**Dependencies**: Relies on session initialization

**Test Session**:
```clojure
;; NOTE: Assumes user gesture completed in Session Initialization
;; Load audio for testing
(audio/load-audio!+ "dev/test-resources/audio-play-test-two-sentences.mp3")

;; Examine play-audio!+ return structure (correct async approach)
(p/let [result (audio/play-audio!+)]
  (def resolved-result result)
  (keys resolved-result))
;; BUG: Contains both :status-before and :status-after

;; Check for race condition behavior rather than timing
(p/let [result (audio/play-audio!+)]
  ;; Examine the structure - the bug is having both status-before and status-after
  {:has-status-before (contains? result :status-before)
   :has-status-after (contains? result :status-after)
   :keys-present (keys result)})
;; CURRENT BEHAVIOR (without user gesture): {:has-status-before false, :has-status-after false, :keys-present ["success" "action" "readiness" "status"]}
;; BUG (with user gesture): Should show :status-before and :status-after keys present
;; EXPECTED AFTER FIX: Only :status-before, or better yet, no immediate status fetching
```

**Expected Behavior (after fix)**:
- ✅ `play-audio!+` should not fetch status immediately after play command
- ✅ Should rely on webview events for status updates
- ✅ No race conditions in status fetching

**Current Behavior (bug)**:
- ❌ Fetches status immediately after sending play command
- ❌ Status-after may not reflect actual audio state
- ❌ Race condition between command dispatch and status fetch

#### Test 4: Resolver ID Validation

**Status**: 🔴 CURRENTLY FAILS - Silent fallback masks ID mismatches
**Dependencies**: Uses current session state (no reset needed)

**Test Session**:
```clojure
;; NOTE: Can use existing session state - no need for fresh webview
;; Check current resolver state as baseline
(def initial-resolvers (get-in @audio/!state [:load-resolvers]))
(def initial-count (count initial-resolvers))

;; Add load with specific ID to existing state
(def test-load (audio/load-audio!+ "dev/test-resources/audio-play-test-very-short.mp3" :id "test-123"))

;; Verify resolver was added correctly
(def updated-resolvers (get-in @audio/!state [:load-resolvers]))
(def updated-count (count updated-resolvers))

;; Test ID validation logic
{:initial-count initial-count
 :updated-count updated-count
 :has-test-123 (contains? updated-resolvers "test-123")
 :resolver-ids (keys updated-resolvers)}

;; BUG: The issue would be in webview message handling where wrong resolvers get used
;; This is more about message routing than state management
;; The silent fallback happens when webview sends back results with wrong/missing IDs
```

**Expected Behavior (after fix)**:
- ✅ Strict ID validation for resolver matching
- ✅ Clear error logging when ID mismatch occurs
- ✅ No silent fallback to "any resolver"

**Current Behavior (bug)**:
- ❌ Falls back to any available resolver when ID doesn't match
- ❌ Silent masking of ID mismatches
- ❌ Could resolve wrong promises#### Test 5: Missing Audio Event Handling

**Status**: 🔴 CURRENTLY FAILS - Missing handlers for some audio events
**Dependencies**: Relies on session initialization

**Test Session**:
```clojure
;; NOTE: This test requires observing webview behavior
;; The bug is that certain audio events don't have handlers
;; We can test this by checking the event registration

;; After Session Initialization, the webview should handle all audio events
;; The specific events missing handlers are: 'waiting', 'stalled', 'suspend'
;; This would manifest as incomplete status updates during network issues

;; Load audio and check status reporting completeness
(audio/load-audio!+ "dev/test-resources/audio-play-test-two-sentences.mp3")

;; The bug would show up as missing status updates for certain audio states
;; This is primarily observable in webview logs and status accuracy
```

**Expected Behavior (after fix)**:
- ✅ Comprehensive event handlers for all relevant audio events
- ✅ Status updates for waiting, stalled, suspend states
- ✅ Better status accuracy during network issues

**Current Behavior (bug)**:
- ❌ Missing event handlers for some audio states
- ❌ Incomplete status tracking
- ❌ Potential status inconsistencies during network issues

#### Test 6: Timeout Error Messages

**Status**: 🔴 CURRENTLY FAILS - Unclear timeout error messages
**Dependencies**: Requires fresh webview state to test timeout behavior cleanly

**Test Session**:
```clojure
;; This test needs fresh state to test timeout handling without interference
(audio/dispose-audio-webview!)
(audio/init-audio-service!)

;; Try to load valid file with very short timeout to trigger timeout handling
(js/Promise.
 (fn [resolve reject]
   (-> (audio/load-audio!+ "dev/test-resources/audio-play-test-two-sentences.mp3" :timeout-ms 1)
       (.then #(resolve (str "Unexpected success: " %)))
       (.catch #(resolve (str "Error message: " (.-message %)))))))
;; BUG: Error message doesn't clearly indicate this was a timeout failure
;; EXPECTED AFTER FIX: Clear timeout indication with duration specified
```

**Expected Behavior (after fix)**:
- ✅ Clear timeout indication in error messages
- ✅ Specific information about timeout duration
- ✅ Different messages for different failure modes

**Current Behavior (bug)**:
- ❌ Error messages don't clearly indicate timeout
- ❌ Ambiguous between timeout and other failures
- ❌ Missing timeout duration in error messages---

## 🔄 SESSION RESET (Verify Clean Shutdown)

**Purpose**: Confirm that the audio service can be cleanly reset for the next testing session.

```clojure
;; Test clean disposal and re-initialization
(audio/dispose-audio-webview!)

;; Verify cleanup
(audio/get-audio-status!+)
;; This should either error (webview gone) or show fresh state

;; Re-initialize fresh
(audio/init-audio-service!)

;; Verify fresh state
(audio/get-audio-status!+)
;; EXPECTED: {:userGestureComplete false, :audioLoaded false, :audioDataReady false, :audioSrc nil}

;; Verify user gesture is reset
(audio/check-user-gesture!+)
;; EXPECTED: false
```

**Expected Behavior**:
- ✅ Clean disposal removes previous webview state
- ✅ Re-initialization creates fresh service
- ✅ User gesture requirement is reset for next session
- ✅ Ready for next testing cycle

**🎯 SESSION COMPLETE**: Audio service reset and ready for next testing cycle.

---

## Running All Tests - Manual Workflow

### 🎯 **Step-by-Step Testing Process**

This is the actual workflow for running comprehensive audio service regression tests. Follow these steps in order:

#### **1. Setup Todo List Tracking**
- Use VS Code todo management tool to track progress
- Mark "Audio Service Regression Test Plan" as in-progress
- All test progress will be visible in VS Code UI

#### **2. Session Initialization** (Human Required)
1. Mark "Session Initialization" as in-progress in todo list
2. Execute Session Initialization code block above
3. **Human**: Click "Enable Audio" when prompted
4. **Human**: Confirm you hear the 6.984s audio playback
5. Mark "Session Initialization" as completed

#### **3. Baseline Tests** (Automated)
Run each test in sequence, updating todo list:

1. **Test A**: Mark in-progress → Execute Duration Detection code → Mark completed
2. **Test B**: Mark in-progress → Execute Fresh State Initialization → Mark completed
3. **Test C**: Mark in-progress → Execute File Existence Validation → Mark completed
4. **Test D**: Mark in-progress → Execute Additional Audio Operations → Mark completed

#### **4. Bug Tests** (Automated)
Run each test in sequence, updating todo list:

1. **Bug Test 1**: Mark in-progress → Execute Concurrent Load Handling → Mark completed
2. **Bug Test 2**: Mark in-progress → Execute Playing Status Accuracy → Mark completed
3. **Bug Test 3**: Mark in-progress → Execute Promise Race Conditions → Mark completed
4. **Bug Test 4**: Mark in-progress → Execute Resolver ID Validation → Mark completed
5. **Bug Test 5**: Mark in-progress → Execute Missing Audio Event Handling → Mark completed
6. **Bug Test 6**: Mark in-progress → Execute Timeout Error Messages → Mark completed

#### **5. Session Reset** (Automated)
1. Mark "Session Reset" as in-progress
2. Execute Session Reset code block
3. Mark "Session Reset" as completed
4. Mark overall "Audio Service Regression Test Plan" as completed

### 📊 **Expected Results**
- **Baseline Tests**: Should all PASS ✅ (protecting existing functionality)
- **Bug Tests**: Currently FAIL 🔴 (will pass after fixes are implemented)
- **Todo List**: Provides clear visual progress throughout testing session**After all fixes are complete:**
- ✅ **Baseline tests should continue to pass** (protecting existing functionality)
- ✅ **Bug tests should now pass** (confirming fixes work)
- ✅ **Duration detection should be preserved** throughout all changes
- ✅ **File validation should remain robust** after fixes

## 🎛️ TESTING WORKFLOW FOR AI AGENTS

### Session-Based Testing Approach

1. **🎛️ Session Initialization** (One-time human interaction):
   - AI loads audio service and requests test file
   - Human clicks "Enable Audio" button once
   - Human confirms audio playback works (validates end-to-end)
   - Service is now ready for automated testing

2. **🤖 Automated Test Execution** (No human interaction needed):
   - AI runs all baseline tests to verify working functionality
   - AI runs all bug tests to expose issues
   - AI checks internal state, promise resolutions, error messages
   - AI validates that working features aren't broken during development

3. **🔄 Session Reset** (Clean shutdown):
   - AI resets service to clean state for next session
   - Verifies fresh initialization works correctly

### Key Benefits:
- **Minimal human interruption**: Front-load all user interactions
- **Uninterrupted testing flow**: Core tests run without human input
- **Better test coverage**: Focus on code behavior rather than UI interactions
- **Reproducible sessions**: Clean initialization and reset for consistency

### AI Agent Guidelines:
- **Always start with Session Initialization** before running bug tests
- **Ask human to verify playback once** during initialization
- **Run automated tests continuously** without stopping for human input
- **Use session reset** to prepare for next testing cycle
- **Remember**: User gesture persists across audio file loads within a session
