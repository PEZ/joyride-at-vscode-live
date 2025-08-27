(ns showtime
  (:require [clojure.string :as string]
            ["vscode" :as vscode]))

;; :timer/type can be :simple or :pausable
;; :simple transitions like this:
;;    :state/reset -> :state/running -> :state/stopped -> :state/reset
;; :pausable transitions like this:
;;    :state/reset -> :state/running -> :state/paused -> :state/running ...

(def empty-timer-state
  "Base timer state structure"
  {:timer/state :state/reset
   :timer/type :simple  ; :simple or :pausable
   :timer/accumulated-ms 0
   :timer/session-start nil
   :timer/last-display "00:00"})

(defonce !state
  (atom {:timer-state empty-timer-state
         :status-item nil
         :update-interval nil
         :emoji "⏱️"
         :debug? false}))

;;;;;;;;;;
;; Pure timer logic, no side effects

(defn timer-elapsed-ms
  "Calculate total elapsed time for a timer state at given timestamp"
  [{:timer/keys [state accumulated-ms session-start]} now-ms]
  (if (and (= state :state/running) session-start)
    (+ accumulated-ms (- now-ms session-start))
    accumulated-ms))

(defn timer-start
  "Start timer from stopped or reset state"
  [timer-state now-ms]
  {:pre [(contains? #{:state/stopped :state/reset} (:timer/state timer-state))]}
  (assoc timer-state
         :timer/state :state/running
         :timer/session-start now-ms))

(defn timer-pause
  "Pause a running timer, accumulating elapsed time"
  [timer-state now-ms]
  {:pre [(= (:timer/state timer-state) :state/running)]}
  (let [elapsed (timer-elapsed-ms timer-state now-ms)]
    (assoc timer-state
           :timer/state :state/paused
           :timer/accumulated-ms elapsed
           :timer/session-start nil)))

(defn timer-resume
  "Resume a paused timer"
  [timer-state now-ms]
  {:pre [(= (:timer/state timer-state) :state/paused)]}
  (assoc timer-state
         :timer/state :state/running
         :timer/session-start now-ms))

(defn timer-reset
  "Reset timer to reset state from any state"
  [timer-state]
  {:timer/state :state/reset
   :timer/type (:timer/type timer-state)  ; Preserve timer type
   :timer/accumulated-ms 0
   :timer/session-start nil
   :timer/last-display "00:00"})

(defn timer-transition
  "Handle timer state transitions"
  [timer-state action now-ms]
  (let [state (:timer/state timer-state)
        timer-type (:timer/type timer-state)]
    (if (= action :click)
      ;; Click actions with timer-type specific behavior
      (case [state timer-type]
        [:state/reset :simple]
        (timer-start timer-state now-ms)

        [:state/running :simple]
        (let [elapsed (timer-elapsed-ms timer-state now-ms)]
          (assoc timer-state
                 :timer/state :state/stopped
                 :timer/accumulated-ms elapsed
                 :timer/session-start nil))

        [:state/stopped :simple]
        (timer-reset timer-state)

        [:state/reset :pausable]
        (timer-start timer-state now-ms)

        [:state/running :pausable]
        (timer-pause timer-state now-ms)

        [:state/paused :pausable]
        (timer-resume timer-state now-ms)

        timer-state) ; default

      ;; Direct actions - state-based, timer-type agnostic
      (case action
        :start (if (contains? #{:state/stopped :state/reset} state)
                 (timer-start timer-state now-ms)
                 timer-state)
        :pause (if (= state :state/running)
                 (timer-pause timer-state now-ms)
                 timer-state)
        :resume (if (= state :state/paused)
                  (timer-resume timer-state now-ms)
                  timer-state)
        :reset (timer-reset timer-state)
        :init empty-timer-state
        timer-state)))) ; default

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
  ;; Let's explore our timer step by step! 🕐

  ;; Base timer structure
  empty-timer-state

  ;; Basic operations
  (timer-start empty-timer-state 1000)
  (timer-elapsed-ms (timer-start empty-timer-state 1000) 3500)
  (timer-pause (timer-start empty-timer-state 1000) 3500)

  ;; Resume from paused
  (let [paused {:timer/state :state/paused :timer/type :simple
                :timer/accumulated-ms 2500 :timer/session-start nil}]
    (timer-resume paused 5000))

  ;; Reset preserves timer type
  (timer-reset {:timer/state :state/running :timer/type :pausable
                :timer/accumulated-ms 42000})

  ;; Time formatting
  (elapsed-ms->time-str 0)
  (elapsed-ms->time-str 15000)
  (elapsed-ms->time-str 65000)
  (elapsed-ms->time-str 3661000)

  ;; Display text (removes leading "00:")
  (timer-display-text {:timer/state :state/running :timer/session-start 1000} 66000)

  ;; Click transitions differ by timer type
  (timer-transition empty-timer-state :click 1000)
  (timer-transition (assoc empty-timer-state :timer/type :pausable) :click 1000)

  ;; Direct actions
  (timer-transition {:timer/state :state/stopped :timer/type :simple} :start 1000)
  (timer-transition {:timer/state :state/running :timer/session-start 1000} :reset 3000)

  ;; Zero padding
  (zero-pad 5)
  (zero-pad 12)

  ;; Threading operations
  (-> empty-timer-state
      (timer-start 1000)
      (timer-pause 3000)
      (timer-elapsed-ms 3000))

  :rcf)

;;;;;;;;;
;; (Side)effectful functions

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
  (let [{:keys [timer-state status-item emoji debug?]} @!state]
    (when status-item
      (let [now (js/Date.now)
            display-text (timer-display-text timer-state now)
            state-indicator (when debug?
                              (case (:timer/state timer-state)
                                :state/running "▶️"
                                :state/paused "⏸️"
                                :state/stopped "⏹️"
                                :state/reset "🔄"))
            text (if debug?
                   (str emoji " " display-text " " state-indicator)
                   (str emoji " " display-text))]
        (set! (.-text status-item) text)))))

(defn start-update-interval!
  "Start interval for live display updates when timer is running"
  []
  (when-let [existing (:update-interval @!state)]
    (js/clearInterval existing))
  (let [interval-id (js/setInterval update-display! 100)]
    (swap! !state assoc :update-interval interval-id)))

(defn stop-update-interval!
  "Stop the display update interval"
  []
  (when-let [interval-id (:update-interval @!state)]
    (js/clearInterval interval-id)
    (swap! !state dissoc :update-interval)))

(defn handle-timer-click!
  "Handle click on timer status item"
  []
  (let [now (js/Date.now)
        current-timer-state (:timer-state @!state)
        new-timer-state (timer-transition current-timer-state :click now)]
    (swap! !state assoc :timer-state new-timer-state)

    (case (:timer/state new-timer-state)
      :state/running (start-update-interval!)
      (stop-update-interval!))

    (update-display!)))

(defn init-timer!
  "Initialize the timer with status bar item and click handler"
  ([]
   (init-timer! {}))
  ([{:keys [timer-type debug? emoji]
     :or {timer-type :simple debug? false}}]
   (let [item (create-timer-item!)
         initial-state (assoc empty-timer-state :timer/type timer-type)]
     (set! (.-command item)
           (clj->js {:command "joyride.runCode"
                     :arguments [(str '(showtime/handle-timer-click!))]}))
     (swap! !state assoc
            :status-item item
            :timer-state initial-state
            :debug? debug?
            :emoji emoji)
     (update-display!)
     item)))

(defn cleanup-timer!
  "Clean up the timer - dispose status item and stop intervals"
  []
  (stop-update-interval!)
  (when-let [item (:status-item @!state)]
    (.dispose item))
  (swap! !state assoc
         :status-item nil
         :timer-state empty-timer-state))

(defn switch-timer-type!
  "Switch between simple and pausable timer types"
  [new-type]
  {:pre [(contains? #{:simple :pausable} new-type)]}
  (swap! !state update :timer-state
         #(assoc (timer-reset %) :timer/type new-type))
  (update-display!)
  (str "Timer switched to " (name new-type) " mode"))

(comment ; a.k.a. A Rich Comment Form (RCF)
  ;; Basic timer usage
  (init-timer!)
  (handle-timer-click!)
  (handle-timer-click!)
  (cleanup-timer!)

  ;; we can switch timer type on the fly
  (switch-timer-type! :pausable)
  (switch-timer-type! :simpe)

  ;; Different timer configurations
  (cleanup-timer!)

  ;; Default: simple timer, no debug, no emoji
  (init-timer!)

  ;; Pausable timer with debug enabled
  (init-timer! {:timer-type :pausable :debug? true})

  ;; Custom emoji with debug
  (init-timer! {:emoji "🕐" :debug? true})

  ;; All options
  (init-timer! {:timer-type :pausable
                :debug? true
                :emoji "🔥"})

  ;; Sports timer style
  (init-timer! {:emoji "⚽" :debug? false})

  ;; Work timer
  (init-timer! {:emoji "💼" :timer-type :pausable})

  ;; Check state
  (let [state @!state]
    {:timer-state (:timer-state state)
     :has-status-item (some? (:status-item state))
     :has-interval (some? (:update-interval state))
     :emoji (:emoji state)
     :debug? (:debug? state)})

  :rcf)