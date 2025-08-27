(ns showtime2
  (:require [clojure.string :as string]
            ["vscode" :as vscode]))

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

(defonce !shell-state
  (atom {:timer-state empty-timer-state
         :status-item nil
         :update-interval nil
         :emoji "⏱️"}))

;;;;;;;;;;
;; Pure timer logic, no side effects

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
      ;; Simple timer behavior
      [:reset :click :simple]     (timer-start timer-state now-ms)
      [:running :click :simple]   (let [elapsed (timer-elapsed-ms timer-state now-ms)]
                                    (assoc timer-state
                                           :timer/state :stopped
                                           :timer/accumulated-ms elapsed
                                           :timer/session-start nil))
      [:stopped :click :simple]   (timer-reset timer-state)

      ;; Pausable timer behavior
      [:reset :click :pausable]   (timer-start timer-state now-ms)
      [:running :click :pausable] (timer-pause timer-state now-ms)
      [:paused :click :pausable]  (timer-resume timer-state now-ms)

      ;; Direct transitions
      [:stopped :start]   (timer-start timer-state now-ms)
      [:reset :start]     (timer-start timer-state now-ms)
      [:running :pause]   (timer-pause timer-state now-ms)
      [:paused :resume]   (timer-resume timer-state now-ms)
      [_ :reset]          (timer-reset timer-state)
      [_ :init]           empty-timer-state

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
  "Format elapsed time for display"
  [timer-state now-ms]
  (let [elapsed-ms (timer-elapsed-ms timer-state now-ms)
        full-text (elapsed-ms->time-str elapsed-ms)]
    (string/replace full-text #"^00:" "")))

(comment ; a.k.a. A Rich Comment Form (RCF)
  ;; Test the functional core
  empty-timer-state

  ;; Test state transitions
  (-> empty-timer-state
      (timer-start 1000)
      (timer-pause 4000))

  ;; Test click behavior cycle
  (let [time-base 1000
        state1 (timer-transition empty-timer-state :click time-base)
        state2 (timer-transition state1 :click (+ time-base 2000))
        state3 (timer-transition state2 :click (+ time-base 5000))]
    {:first-click state1
     :second-click state2
     :third-click state3})

  ;; Test time formatting
  (elapsed-ms->time-str 0)
  (elapsed-ms->time-str 15000)
  (elapsed-ms->time-str 65000)
  (elapsed-ms->time-str 3661000)

  :rcf)

;;;;;;;;;
;; (Side) effectful functions

(defn create-timer-item!
  "Create a VS Code status bar item for the timer"
  []
  (let [item (vscode/window.createStatusBarItem
              vscode/StatusBarAlignment.Left
              -999)]
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
  "Handle click on timer status item"
  []
  (let [now (js/Date.now)
        current-timer-state (:timer-state @!shell-state)
        new-timer-state (timer-transition current-timer-state :click now)]
    (swap! !shell-state assoc :timer-state new-timer-state)

    (case (:timer/state new-timer-state)
      :running (start-update-interval!)
      (stop-update-interval!))

    (update-display!)))

(defn init-timer!
  "Initialize the timer with status bar item and click handler"
  []
  (let [item (create-timer-item!)]
    (set! (.-command item)
          (clj->js {:command "joyride.runCode"
                    :arguments ["(showtime2/handle-timer-click!)"]}))
    (swap! !shell-state assoc
           :status-item item
           :timer-state empty-timer-state)
    (update-display!)
    item))

(defn cleanup-timer!
  "Clean up the timer - dispose status item and stop intervals"
  []
  (stop-update-interval!)
  (when-let [item (:status-item @!shell-state)]
    (.dispose item))
  (swap! !shell-state assoc
         :status-item nil
         :timer-state empty-timer-state))

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

(comment ; a.k.a. A Rich Comment Form (RCF)
  (init-timer!)
  (handle-timer-click!)
  (handle-timer-click!)
  (cleanup-timer!)

  ;; Check state
  (let [state @!shell-state]
    {:timer-state (:timer-state state)
     :has-status-item (some? (:status-item state))
     :has-interval (some? (:update-interval state))
     :emoji (:emoji state)})

  :rcf)