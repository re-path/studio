(ns main
  (:require
   #_["@sentry/electron/main" :as sentry-electron-main]
   ["electron-extension-installer" :refer [REACT_DEVELOPER_TOOLS]]
   ["electron-extension-installer$default" :as installExtension]
   #_["electron-updater" :as updater]
   ["electron-window-state" :as window-state-keeper]
   ["electron" :refer [app shell ipcMain BrowserWindow clipboard nativeTheme]]
   ["path" :as path]
   [config]
   [file]))

(def main-window (atom nil))
(def loading-window (atom nil))

(defn to-main-api
  [args]
  (case (.-action args)
    "windowMinimize" (.minimize ^js @main-window)
    "windowToggleMaximized" (if (.isMaximized ^js @main-window)
                              (.unmaximize ^js @main-window)
                              (.maximize ^js @main-window))
    "windowToggleFullscreen" (.setFullScreen
                              ^js @main-window
                              (not (.isFullScreen ^js @main-window)))
    "setThemeMode" (set! (.. nativeTheme -themeSource) (.-data args))
    "openRemoteUrl" (.openExternal shell (.-data args))
    ;; https://www.electronjs.org/docs/api/clipboard#clipboardwritedata-type
    "writeToClipboard" (.write clipboard (.-data args))
    "openDocument" (file/open)
    "saveDocument" (file/save (.-data args))
    "export" (file/export (.-data args))))

(defn send-to-renderer
  ([action]
   (send-to-renderer action nil))
  ([action data]
   (.send (.-webContents ^js @main-window) "fromMain" (clj->js {:action action
                                                                :data data}))))

(defn init-main-window
  []
  (let [win-state (window-state-keeper #js {:defaultWidth 1920
                                            :defaultHeight 1080})]
    (reset! main-window
            (BrowserWindow.
             #js {:x (.-x win-state)
                  :y (.-y win-state)
                  :width (.-width win-state)
                  :height (.-height win-state)
                  :backgroundColor "#313131"
                  :icon (.join path js/__dirname "/public/img/icon.png")
                  :frame false
                  :show false
                  :webPreferences
                  #js {:devTools config/debug?
                       :sandbox false
                       :preload (.join path js/__dirname "preload.js")}}))

    (.once ^js @main-window "ready-to-show"
           (fn []
             (.show ^js @main-window)
             (.manage win-state ^js @main-window)
             (send-to-renderer (if (.isMaximized ^js @main-window)
                                 "windowMaximized"
                                 "windowUnmaximized"))
             (send-to-renderer (if (.isFullScreen ^js @main-window)
                                 "windowEnteredFullscreen"
                                 "windowLeavedFullscreen"))
             (.hide ^js @loading-window)
             (.close ^js @loading-window)))

    (.loadURL ^js @main-window (if config/debug?
                                 "http://localhost:8080"
                                 (.join path "file://" js/__dirname "/public/index.html")))


    (when config/debug?
      (-> (installExtension
           REACT_DEVELOPER_TOOLS
           #js {:loadExtensionOptions {:allowFileAccess true}})
          (.then (fn [name] (js/console.log "Added Extension: " name)))
          (.catch (fn [err] (js/console.log "An error occurred: " err))))
      (.openDevTools (.-webContents ^js @main-window)))

    (doseq
     [[web-contents-event f]
      [["will-navigate"  #(.preventDefault %)] ;; Prevent navigation
       ["new-window" #(.preventDefault %)] ;; Prevent popups
       ["closed" #(reset! main-window nil)]]]
      (.on (.-webContents ^js @main-window) web-contents-event f))

    (.on ipcMain "toMain" #(to-main-api %2))

    (doseq
     [[window-event action]
      [;; Event "resized" is more suitable, but it's not supported on linux
       ["resize" #(if (.isMaximized ^js @main-window)
                    "windowMaximized"
                    "windowUnmaximized")]
       ["maximize" "windowMaximized"]
       ["unmaximize" "windowUnmaximized"]
       ["enter-full-screen" "windowEnteredFullscreen"]
       ["leave-full-screen" "windowLeavedFullscreen"]
       ["minimize" "windowMinimized"]
       ["restore" "windowRestored"]]]
      (.on ^js @main-window window-event #(send-to-renderer action)))

    #_(.checkForUpdatesAndNotify updater)))

(defn init-loading-window []
  (set! (.-allowRendererProcessReuse app) false)
  (reset! loading-window
          (BrowserWindow.
           #js {:width 720
                :height 576
                :backgroundColor "#313131"
                :icon (.join path js/__dirname "/public/img/icon.png")
                :show false
                :frame false}))
  (.once ^js @loading-window "show" init-main-window)
  (.loadURL ^js @loading-window (.join path "file://" js/__dirname "/public/loading.html"))
  (.once ^js (.-webContents @loading-window) "did-finish-load" #(.show ^js @loading-window)))

(defn ^:export init []
  #_(sentry-electron-main/init (clj->js config/sentry-options))
  (.on app "window-all-closed" #(when-not (= js/process.platform "darwin")
                                  (.quit app)))
  (.on app "ready" init-loading-window))
