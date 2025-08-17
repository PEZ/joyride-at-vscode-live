(ns ai-presenter.audio-playback
  "Core playback module."
  (:require ["vscode" :as vscode]
            ["path" :as path]
            [promesa.core :as p]
            [joyride.core :as joy]
            ["fs" :as fs]))

(def resource-dir (path/join
                   (-> vscode/workspace.workspaceFolders
                       first
                       .-uri
                       .-fsPath)
                   ".joyride"
                   "resources"))

(defonce !state (atom {:webview nil
                       :status-resolvers {}
                       :current-load-resolver nil  ; Single resolver for the one allowed load operation
                       :last-known-status nil}))

;; =============================================================================
;; PURE FUNCTIONS - Functional Core
;; =============================================================================

(defn ensure-absolute-path
  "Convert relative path to absolute path if needed, and verify existence"
  [file-path]
  (let [absolute-path (if (.startsWith file-path "/")
                        file-path
                        (path/join (-> vscode/workspace.workspaceFolders
                                       first
                                       .-uri
                                       .-fsPath)
                                   file-path))]
    (if (fs/existsSync absolute-path)
      absolute-path
      (throw (js/Error. (str "File or directory does not exist: '" file-path "' "
                             "(resolved to: " absolute-path ")"))))))

(defn can-play?
  "Pure function: Check if audio system is ready to play"
  [status]
  (and (:userGestureComplete status)
       (:audioLoaded status)))

(defn add-status-resolver
  "Pure function: Add a status resolver to state"
  [state id resolver]
  (assoc-in state [:status-resolvers id] resolver))

(defn add-load-resolver
  "Pure function: Set the current load resolver (only one allowed)"
  [state resolver-map]
  (assoc state :current-load-resolver resolver-map))

(defn remove-resolver
  "Pure function: Remove a resolver from state"
  [state resolver-type id]
  (if (= resolver-type :current-load-resolver)
    (assoc state :current-load-resolver nil)
    (update state resolver-type dissoc id)))

(defn clear-current-load-resolver
  "Pure function: Clear the current load resolver"
  [state]
  (assoc state :current-load-resolver nil))

(defn update-last-status
  "Pure function: Update the cached status"
  [state new-status]
  (assoc state :last-known-status new-status))

(defn get-play-readiness
  "Pure function: Analyze play readiness and return info"
  [status]
  (cond
    (not (:userGestureComplete status))
    {:ready? false
     :reason :no-user-gesture
     :message "Please click Enable Audio first"}

    (not (:audioLoaded status))
    {:ready? false
     :reason :audio-not-loaded
     :message "Audio not yet loaded/ready"}

    :else
    {:ready? true
     :reason :ready
     :message "Ready to play"}))

;; =============================================================================
;; IMPERATIVE SHELL - Side Effects
;; =============================================================================

(defn dispose-audio-webview! []
  (when-let [webview (:webview @!state)]
    (.dispose webview)
    (swap! !state assoc :webview nil)))

(defn create-audio-webview! []
  (dispose-audio-webview!)
  (let [webview (vscode/window.createWebviewPanel
                 "audio-service-webview"
                 "Audio Service"
                 (.-One vscode/ViewColumn)
                 (clj->js {:enableScripts true
                           :retainContextWhenHidden true}))]
    (swap! !state assoc :webview webview)
    webview))

(defn load-html-from-file!
  "Load HTML content from external file to avoid string parsing issues"
  []
  (when-let [webview (:webview @!state)]
    (let [html-path (path/join resource-dir "audio-service.html")
          html-content (fs/readFileSync html-path "utf8")]
      (set! (-> webview .-webview .-html) html-content))))

(defn init-audio-service!
  "Initialize the audio service webview and handlers"
  []
  (create-audio-webview!)
  (load-html-from-file!)
  (.onDidReceiveMessage
   (.-webview (:webview @!state))
   (fn [message]
     (println "audio-service-webview message:" message)
     ;; Handle status responses
     (when (= (.-type message) "statusResponse")
       (let [status (js->clj (.-status message) :keywordize-keys true)
             current-state @!state]
         ;; Update cached status using pure function
         (swap! !state update-last-status status)
         ;; Resolve pending status request
         (when-let [resolver (get-in current-state [:status-resolvers "current"])]
           (resolver status)
           (swap! !state remove-resolver :status-resolvers "current"))))
     ;; Handle audio ready notifications
     (when (= (.-type message) "audioReady")
       (println "🎵 Audio ready notification received!")
       (let [load-data (js->clj message :keywordize-keys true)
             current-state @!state
             audio-id (or (:id load-data) "default")]
         ;; Check if we have a current load resolver and if IDs match
         (if-let [resolver-map (:current-load-resolver current-state)]
           (if (= (:id resolver-map) audio-id)
             (do
               ((:resolve resolver-map) load-data)
               (swap! !state clear-current-load-resolver))
             (let [error-msg (str "Audio ready ID mismatch: expected '" (:id resolver-map) "' but got '" audio-id "'")]
               (println "❌" error-msg)
               (vscode/window.showWarningMessage error-msg)
               ;; Reject the current resolver with clear error
               ((:reject resolver-map) (js/Error. error-msg))
               (swap! !state clear-current-load-resolver)))
           (let [error-msg (str "Audio ready notification for ID '" audio-id "' but no current load resolver")]
             (println "❌" error-msg)
             (vscode/window.showWarningMessage error-msg)))))
     ;; Handle audio load error notifications
     (when (= (.-type message) "audioLoadError")
       (println "❌ Audio load error notification received!")
       (let [error-data (js->clj message :keywordize-keys true)
             current-state @!state
             audio-id (or (:id error-data) "default")]
         ;; Check if we have a current load resolver and if IDs match
         (if-let [resolver-map (:current-load-resolver current-state)]
           (if (= (:id resolver-map) audio-id)
             (do
               ((:reject resolver-map) (js/Error. (:error error-data)))
               (swap! !state clear-current-load-resolver))
             (let [error-msg (str "Audio error ID mismatch: expected '" (:id resolver-map) "' but got '" audio-id "'")]
               (println "❌" error-msg)
               (vscode/window.showWarningMessage error-msg)
               ;; Still reject the current resolver since something went wrong
               ((:reject resolver-map) (js/Error. (str error-msg ". Original error: " (:error error-data))))
               (swap! !state clear-current-load-resolver)))
           (let [error-msg (str "Audio error notification for ID '" audio-id "' but no current load resolver")]
             (println "❌" error-msg)
             (vscode/window.showWarningMessage error-msg)))))
     message))
  (:webview @!state))

(defn send-audio-command! [command & args]
  (when-let [webview (:webview @!state)]
    (.postMessage
     (.-webview webview)
     (clj->js (apply merge {:command (name command)} args)))))

(defn get-audio-status!+ []
  (p/create
   (fn [resolve reject]
     ;; Store the resolver using pure function
     (swap! !state add-status-resolver "current" resolve)
     ;; Request status from webview
     (send-audio-command! :status)
     ;; Timeout after 5 seconds
     (js/setTimeout #(do
                       (swap! !state remove-resolver :status-resolvers "current")
                       (reject "Status request timeout")) 5000))))

(defn play-audio!+
  "Smart play that checks readiness first and returns comprehensive info"
  [& {:keys [id]}]
  (p/let [status (get-audio-status!+)
          readiness (get-play-readiness status)]
    (if (:ready? readiness)
      (do
        (send-audio-command! :play {:id (or id "default")})
        {:success true
         :action :played
         :readiness readiness
         :status-before status})
      {:success false
       :action :blocked
       :readiness readiness
       :status status})))

(defn pause-audio!+ [& {:keys [id]}]
  (send-audio-command! :pause {:id (or id "default")}))

(defn stop-audio!+ [& {:keys [id]}]
  (send-audio-command! :stop {:id (or id "default")}))

(defn set-volume!+ [volume & {:keys [id]}]
  (send-audio-command! :volume {:volume volume :id (or id "default")}))

(defn load-audio!+
  "Returns a promise that resolves when audio is loaded and ready to play, or rejects with detailed error info"
  [local-file-path & {:keys [id timeout-ms] :or {timeout-ms 10000}}]
  (try
    (let [audio-id (or id "default")
          absolute-path (ensure-absolute-path local-file-path)]
      ;; Reject any existing load operation
      (when-let [{:keys [reject]} (:current-load-resolver @!state)]
        (reject (js/Error. "Load cancelled by new load operation")))
      (swap! !state clear-current-load-resolver)
      (p/create
       (fn [resolve reject]
         ;; Store both resolve and reject functions for the single allowed load
         (swap! !state add-load-resolver {:resolve resolve :reject reject :id audio-id})
         ;; Send load command using existing function
         (let [webview (:webview @!state)
               audio-uri (.asWebviewUri (.-webview webview) (vscode/Uri.file absolute-path))]
           (send-audio-command! :load {:audioPath (str audio-uri)
                                       :id audio-id}))
         ;; Enhanced timeout with status check
         (js/setTimeout
          #(p/let [final-status (get-audio-status!+)]
             (swap! !state clear-current-load-resolver)
             (if (and (:audioDataReady final-status)
                      (not (:userGestureComplete final-status)))
               ;; Audio data loaded but waiting for user gesture
               (reject (js/Error.
                        (str "Audio loaded but requires user gesture. Duration: "
                             (:audioDuration final-status) "s. Please click 'Enable Audio'.")))
               ;; True timeout or other issue
               (reject (js/Error.
                        (str "Audio load timeout for: " local-file-path
                             ". Status: " (:playbackState final-status)
                             (when (:lastError final-status)
                               (str ". Error: " (:lastError final-status))))))))
          timeout-ms))))
    (catch :default e
      (vscode/window.showErrorMessage (.-message e))
      (p/reject! e))))

(defn check-user-gesture!+
  "Check if user gesture has been completed"
  []
  (p/let [status (get-audio-status!+)]
    (:userGestureComplete status)))

(defn prompt-user-for-audio-gesture!+
  "Show user a message to enable audio and wait for confirmation"
  []
  (p/create
   (fn [resolve reject]
     (-> (vscode/window.showInformationMessage
          "🔊 Please enable audio in the webview by clicking the 'Enable Audio' button, then click Done."
          "Done")
         (.then (fn [selection]
                  (if (= selection "Done")
                    (resolve true)
                    (reject (js/Error. "User cancelled audio setup")))))))))

(defn load-and-play-audio!+
  "Load and play audio with proper user gesture checking"
  [file-path]
  (p/let [gesture-complete? (check-user-gesture!+)]
    (if gesture-complete?
      ;; User gesture already complete, proceed with loading then playing
      (p/let [load-result (load-audio!+ file-path)
              play-result (play-audio!+)]
        {:load-result load-result
         :play-result play-result
         :success true})
      ;; No user gesture yet, prompt user first then load and play
      (p/let [_ (prompt-user-for-audio-gesture!+)
              ;; After user clicks Done, proceed with loading then playing
              load-result (load-audio!+ file-path)
              play-result (play-audio!+)]
        {:load-result load-result
         :play-result play-result
         :success true}))))

(comment
  (p/let [load+ (load-audio!+ "dev/test-resources/audio-play-test-very-short.mp3")]
    (def load+ load+))


  (p/let [play+ (play-audio!+)]
    (def play+ play+))

  (p/let [load-and-play+ (load-and-play-audio!+ "dev/test-resources/audio-play-test-two-sentences.mp3")]
    (def load-and-play+ load-and-play+))

  (p/let [pause+ (pause-audio!+)]
    (def pause+ pause+))

  (p/let [set-volume+ (set-volume!+ 0.1)]
    (def set-volume+ set-volume+))

  (p/let [stop+ (stop-audio!+)]
    (def stop+ stop+))

  (load-audio!+ "not-a-path/not-a-file.mp3")

  (p/let [_ (load-audio!+ "not-a-path/not-a-file.mp3")])

  :rcf)