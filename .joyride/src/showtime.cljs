(ns showtime
  (:require [clojure.string :as string]
            ["vscode" :as vscode]))

;; :timer/type can be :timer-type/stoppable or :timer-type/pausable
;; :timer-type/stoppable transitions like this:
;;    :state/reset -> :state/running -> :state/stopped -> :state/reset
;; :timer-type/pausable transitions like this:
;;    :state/reset -> :state/running -> :state/paused -> :state/running ...

(def empty-timer-state
  "Base timer state structure"
  {:timer/state :state/reset
   :timer/timer-type :timer-type/stoppable  ; :timer-type/stoppable or :timer-type/pausable
   :timer/accumulated-ms 0
   :timer/session-start nil
   :timer/last-display "00:00"})

(defonce !state
  (atom {:app/timer empty-timer-state
         :app/status-item nil
         :app/update-interval nil
         :app/emoji "⏱️"
         :app/debug? false}))

;;;;;;;;;;
;; util

(defn deep-merge [m1 m2]
  (reduce (fn [acc [k v]]
            (if (and (map? (get acc k))
                     (map? v))
              (assoc acc k (deep-merge (get acc k) v))
              (assoc acc k v)))
          m1
          m2))

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
   :timer/timer-type (:timer/timer-type timer-state)  ; Preserve timer type
   :timer/accumulated-ms 0
   :timer/session-start nil
   :timer/last-display "00:00"})

(defn timer-transition
  "Handle timer state transitions"
  [timer-state action now-ms]
  (let [state (:timer/state timer-state)
        timer-type (:timer/timer-type timer-state)]
    (if (= action :click)
      ;; Click actions with timer-type specific behavior
      (case [state timer-type]
        [:state/reset :timer-type/stoppable]
        (timer-start timer-state now-ms)

        [:state/running :timer-type/stoppable]
        (let [elapsed (timer-elapsed-ms timer-state now-ms)]
          (assoc timer-state
                 :timer/state :state/stopped
                 :timer/accumulated-ms elapsed
                 :timer/session-start nil))

        [:state/stopped :timer-type/stoppable]
        (timer-reset timer-state)

        [:state/reset :timer-type/pausable]
        (timer-start timer-state now-ms)

        [:state/running :timer-type/pausable]
        (timer-pause timer-state now-ms)

        [:state/paused :timer-type/pausable]
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
  (let [paused {:timer/state :state/paused :timer/timer-type :timer-type/stoppable
                :timer/accumulated-ms 2500 :timer/session-start nil}]
    (timer-resume paused 5000))

  ;; Reset preserves timer type
  (timer-reset {:timer/state :state/running :timer/timer-type :timer-type/pausable
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
  (timer-transition (assoc empty-timer-state :timer/timer-type :timer-type/pausable) :click 1000)

  ;; Direct actions
  (timer-transition {:timer/state :state/stopped :timer/timer-type :timer-type/stoppable} :start 1000)
  (timer-transition {:timer/state :state/running :timer/session-start 1000} :reset 3000)

  ;; Zero padding
  (zero-pad 5)
  (zero-pad 12)

  ;; Composable timer state transitions: start → pause → query elapsed time
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
  [state]
  (let [{:app/keys [timer status-item emoji debug?]} state]
    (when status-item
      (let [now (js/Date.now)
            display-text (timer-display-text timer now)
            state-indicator (when debug?
                              (case (:timer/state timer)
                                :state/running "▶️"
                                :state/paused "⏸️"
                                :state/stopped "⏹️"
                                :state/reset "🔄"))
            text (if debug?
                   (str emoji " " display-text " " state-indicator)
                   (str emoji " " display-text))]
        (set! (.-text status-item) text)
        state))))

(defn start-update-interval!
  "Start interval for live display updates when timer is running"
  [state]
  (when-let [existing (:app/update-interval state)]
    (js/clearInterval existing))
  (let [interval-id (js/setInterval #(update-display! @!state) 100)]
    (swap! !state assoc :app/update-interval interval-id)))

(defn stop-update-interval!
  "Stop the display update interval"
  [state]
  (when-let [interval-id (:app/update-interval state)]
    (js/clearInterval interval-id)
    (swap! !state dissoc :app/update-interval)))

(defn handle-timer-click!
  "Handle click on timer status item"
  []
  (let [now (js/Date.now)
        current-timer-state (:app/timer @!state)
        new-timer-state (timer-transition current-timer-state :click now)]
    (swap! !state assoc :app/timer new-timer-state)

    (case (:timer/state new-timer-state)
      :state/running (start-update-interval! @!state)
      (stop-update-interval! @!state))

    (update-display! @!state)))

(defn init-timer!
  "Initialize the timer with status bar item and click handler"
  ([]
   (init-timer! empty-timer-state))
  ([state]
   (let [item (create-timer-item!)]
     (set! (.-command item)
           (clj->js {:command "joyride.runCode"
                     :arguments [(str '(showtime/handle-timer-click!))]}))

     (update-display! (reset! !state (merge {:app/status-item item
                                           :app/timer empty-timer-state}
                                           state)))
     item)))

(defn cleanup-timer!
  "Clean up the timer - dispose status item and stop intervals"
  []
  (stop-update-interval! @!state)
  (when-let [item (:app/status-item @!state)]
    (.dispose item))
  (swap! !state assoc
         :app/status-item nil
         :app/timer empty-timer-state))

(defn merge-app-state!
  "Switch between stoppable and pausable timer types"
  [!new-state]

  (update-display! (swap! !state deep-merge !new-state)))

(comment ; a.k.a. A Rich Comment Form (RCF)
  ;; Basic timer usage
  (init-timer!)
  (handle-timer-click!)
  (handle-timer-click!)
  (cleanup-timer!)

  ;; we can update app state on the fly
  (merge-app-state! {:app/timer {:timer/timer-type
                                 :timer-type/pausable}})
  (merge-app-state! {:app/emoji "🎸"})

  ;; Different timer configurations
  (cleanup-timer!)

  ;; Default: stoppable timer, no debug, no emoji
  (init-timer!)

  ;; Pausable timer with debug enabled
  (init-timer! {:app/timer {:timer/timer-type
                            :timer-type/pausable}
                :app/debug? true})

  ;; Custom emoji with debug
  (init-timer! {:app/emoji "🕐" :app/debug? true})

  ;; All options
  (init-timer! {:app/timer {:timer/timer-type
                            :timer-type/stoppable}
                :app/debug? true
                :app/emoji "🔥"})

  ;; Sports timer style
  (init-timer! {:app/emoji "⚽" :app/debug? false})

  ;; Work timer
  (init-timer! {:app/emoji "💼" :app/timer {:timer/timer-type
                                            :timer-type/pausable}})

  ;; Check state
  (let [state @!state]
    {:app/timer (:app/timer state)
     :has-status-item (some? (:app/status-item state))
     :has-interval (some? (:app/update-interval state))
     :app/emoji (:app/emoji state)
     :app/debug? (:app/debug? state)})

  :rcf)