(ns showtime2
  (:require [clojure.string :as string]
            ["vscode" :as vscode]))

;; =============================================================================
;; FUNCTIONAL CORE - Pure timer logic, no side effects
;; =============================================================================

;; :timer/type can be :simple or :pausable
;; :simple transitions like this:
;;    :reset -> :running -> :stopped -> :reset
;; :pausable transitions like this:
;;    :reset -> :running -> :paused -> :running ...
(def empty-timer-state
  "Base timer state structure"
  {:timer/state :reset
   :timer/type :simple  ; :simple or :pausable
   :timer/accumulated-ms 0
   :timer/session-start nil
   :timer/last-display "00:00"})

(defn timer-init
  "Initialize a fresh timer state"
  []
  empty-timer-state)

(defn timer-init-with-type
  "Initialize a timer with specific type (:simple or :pausable)"
  [timer-type]
  (assoc empty-timer-state :timer/type timer-type))

(defn timer-elapsed-ms
  "Calculate total elapsed time for a timer state at given timestamp"
  [{:timer/keys [state accumulated-ms session-start]} now-ms]
  (if (and (= state :running) session-start)
    (+ accumulated-ms (- now-ms session-start))
    accumulated-ms))

(defn timer-start
  "Start timer from stopped or reset state"
  [timer-state now-ms]
  {:pre [(contains? #{:stopped :reset} (:timer/state timer-state))]}
  (assoc timer-state
         :timer/state :running
         :timer/session-start now-ms))

(defn timer-pause
  "Pause a running timer, accumulating elapsed time"
  [timer-state now-ms]
  {:pre [(= (:timer/state timer-state) :running)]}
  (let [elapsed (timer-elapsed-ms timer-state now-ms)]
    (assoc timer-state
           :timer/state :paused
           :timer/accumulated-ms elapsed
           :timer/session-start nil)))

(defn timer-resume
  "Resume a paused timer"
  [timer-state now-ms]
  {:pre [(= (:timer/state timer-state) :paused)]}
  (assoc timer-state
         :timer/state :running
         :timer/session-start now-ms))

(defn timer-reset
  "Reset timer to reset state from any state"
  [timer-state]
  {:timer/state :reset
   :timer/type (:timer/type timer-state)  ; Preserve timer type
   :timer/accumulated-ms 0
   :timer/session-start nil
   :timer/last-display "00:00"})

(defn timer-transition
  "Handle timer state transitions based on action and current state"
  [timer-state action now-ms]
  (let [timer-type (:timer/type timer-state)]
    (case [(:timer/state timer-state) action timer-type]
      ;; Simple timer behavior: reset -> running -> stopped -> reset
      [:reset :click :simple]     (timer-start timer-state now-ms)   ; reset -> running
      [:running :click :simple]   (let [elapsed (timer-elapsed-ms timer-state now-ms)]
                                    (assoc timer-state
                                           :timer/state :stopped
                                           :timer/accumulated-ms elapsed
                                           :timer/session-start nil))  ; running -> stopped (preserve time)
      [:stopped :click :simple]   (timer-reset timer-state)          ; stopped -> reset

      ;; Pausable timer behavior: reset -> running <-> paused (with separate reset)
      [:reset :click :pausable]   (timer-start timer-state now-ms)   ; reset -> running
      [:running :click :pausable] (timer-pause timer-state now-ms)   ; running -> paused
      [:paused :click :pausable]  (timer-resume timer-state now-ms)  ; paused -> running

      ;; Direct transitions (work for both types):
      [:stopped :start]   (timer-start timer-state now-ms)
      [:reset :start]     (timer-start timer-state now-ms)
      [:running :pause]   (timer-pause timer-state now-ms)
      [:paused :resume]   (timer-resume timer-state now-ms)
      [_ :reset]          (timer-reset timer-state)
      [_ :init]           (timer-init)

      ;; Default: no change
      timer-state)))

(defn zero-pad
  "Add leading zero if needed"
  [x]
  (str (when (< x 10) "0") x))

(defn elapsed-ms->time-str
  "Convert elapsed milliseconds to HH:MM:SS format"
  [ms]
  (let [seconds (int (/ ms 1000))
        minutes (int (/ seconds 60))
        hours (int (/ minutes 60))
        sec-remainder (mod seconds 60)
        min-remainder (mod minutes 60)]
    (str (zero-pad hours) ":"
         (zero-pad min-remainder) ":"
         (zero-pad sec-remainder))))

(defn timer-display-text
  "Format elapsed time for display, matching original showtime.cljs behavior"
  [timer-state now-ms]
  (let [elapsed-ms (timer-elapsed-ms timer-state now-ms)
        full-text (elapsed-ms->time-str elapsed-ms)]
    ;; Match original behavior: remove only leading hour if it's 00, keep MM:SS
    (string/replace full-text #"^00:" "")))

;; =============================================================================
;; IMPERATIVE SHELL - Side effects coordination
;; =============================================================================

(defonce !shell-state
  (atom {:timer-state (timer-init)
         :status-item nil
         :update-interval nil
         :emoji "⏱️"}))

(defn create-timer-item!
  "Create a VS Code status bar item for the new timer"
  []
  (let [item (vscode/window.createStatusBarItem
              vscode/StatusBarAlignment.Left
              -999)]  ; Different priority than original (-1000)
    (.show item)
    item))

(defn update-display!
  "Update the status bar item with current timer state"
  []
  (let [{:keys [timer-state status-item emoji]} @!shell-state]
    (when status-item
      (let [now (js/Date.now)
            display-text (timer-display-text timer-state now)
            state-indicator (case (:timer/state timer-state)
                             :running "▶️"
                             :paused "⏸️"
                             :stopped "⏹️"
                             :reset "🔄")]
        (set! (.-text status-item)
              (str emoji " " display-text " " state-indicator))))))

(defn start-update-interval!
  "Start interval for live display updates when timer is running"
  []
  (when-let [existing (:update-interval @!shell-state)]
    (js/clearInterval existing))
  (let [interval-id (js/setInterval update-display! 100)]
    (swap! !shell-state assoc :update-interval interval-id)))

(defn stop-update-interval!
  "Stop the display update interval"
  []
  (when-let [interval-id (:update-interval @!shell-state)]
    (js/clearInterval interval-id)
    (swap! !shell-state dissoc :update-interval)))

(defn handle-timer-click!
  "Handle click on timer status item using functional core"
  []
  (let [now (js/Date.now)
        current-timer-state (:timer-state @!shell-state)
        new-timer-state (timer-transition current-timer-state :click now)]
    (swap! !shell-state assoc :timer-state new-timer-state)

    ;; Manage update interval based on new state
    (case (:timer/state new-timer-state)
      :running (start-update-interval!)
      (stop-update-interval!))

    (update-display!)))

(defn init-new-timer!
  "Initialize the new timer with status bar item and click handler"
  []
  (let [item (create-timer-item!)]
    (set! (.-command item)
          (clj->js {:command "joyride.runCode"
                    :arguments ["(showtime2/handle-timer-click!)"]}))
    (swap! !shell-state assoc
           :status-item item
           :timer-state (timer-init))
    (update-display!)
    item))

(defn cleanup-new-timer!
  "Clean up the new timer - dispose status item and stop intervals"
  []
  (stop-update-interval!)
  (when-let [item (:status-item @!shell-state)]
    (.dispose item))
  (swap! !shell-state assoc
         :status-item nil
         :timer-state (timer-init)))
(defn switch-timer-type!
  "Switch between simple and pausable timer types"
  [new-type]
  {:pre [(contains? #{:simple :pausable} new-type)]}
  (swap! !shell-state update :timer-state
         #(assoc (timer-reset %) :timer/type new-type))
  (update-display!)
  (str "Timer switched to " (name new-type) " mode"))

(defn make-pausable-timer!
  "Switch current timer to pausable mode"
  []
  (switch-timer-type! :pausable))

(defn make-simple-timer!
  "Switch current timer to simple mode"
  []
  (switch-timer-type! :simple))


;; =============================================================================
;; RICH COMMENT FORMS - REPL-driven development and testing
;; =============================================================================

(comment
  ;; Test the functional core in isolation
  (timer-init)

  ;; Test state transitions
  (-> (timer-init)
      (timer-start 1000)
      (timer-pause 4000))  ; Should accumulate 3000ms

  ;; Test click behavior cycle
  (let [time-base 1000
        state1 (timer-transition (timer-init) :click time-base)         ; stopped -> start
        state2 (timer-transition state1 :click (+ time-base 2000))      ; running -> pause
        state3 (timer-transition state2 :click (+ time-base 5000))]     ; paused -> resume
    {:first-click state1
     :second-click state2
     :third-click state3})

  ;; Test time formatting
  (elapsed-ms->time-str 0)        ; "00:00:00"
  (elapsed-ms->time-str 15000)    ; "00:00:15"
  (elapsed-ms->time-str 65000)    ; "00:01:05"
  (elapsed-ms->time-str 3661000)  ; "01:01:01"

  ;; Test display formatting (removes leading zeros)
  (timer-display-text {:timer/state :running
                       :timer/accumulated-ms 0
                       :timer/session-start 1000} 1015000)  ; Should show "15"

  ;; Demo the complete timer system
  (init-new-timer!)           ; Initialize new timer
  (handle-timer-click!)       ; Start timer
  (handle-timer-click!)       ; Pause timer
  (handle-timer-click!)       ; Resume timer
  (cleanup-new-timer!)        ; Clean up when done

  ;; Check state (avoiding circular reference serialization)
  (let [state @!shell-state]
    {:timer-state (:timer-state state)
     :has-status-item (some? (:status-item state))
     :has-interval (some? (:update-interval state))
     :emoji (:emoji state)})

  :rcf)