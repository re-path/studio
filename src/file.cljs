(ns file
  (:require
   ["electron" :refer [app dialog]]
   ["fs" :as fs]
   ["path" :as path]
   [clojure.edn :as edn]))

(def dialog-options
  {:defaultPath (.getPath app "documents")
   ;; https://www.electronjs.org/docs/api/structures/file-filter#filefilter-object
   :filters [{:name "rp"
              :extensions ["rp"]}]})

(defn- serialize-document
  [data file-path]
  (pr-str (assoc data
                 :path file-path
                 :title (.basename path file-path))))

(defn- write-file
  "https://nodejs.org/en/learn/manipulating-files/writing-files-with-nodejs"
  [file-path data f]
  (.writeFile fs file-path (pr-str (dissoc data :closing?)) #js {:encoding "utf-8"}
              (fn [_err] (f (serialize-document data file-path)))))

(defn- read-file
  "https://nodejs.org/en/learn/manipulating-files/reading-files-with-nodejs"
  [file-path f]
  (.readFile fs file-path #js {:encoding "utf-8"}
             (fn [_err data]
               (let [document (edn/read-string data)]
                 (f (serialize-document document file-path))))))

(defn save-as
  "Saves the provided data.
   
   If there is no path defined, pick a new file.
   https://www.electronjs.org/docs/api/dialog#dialogshowsavedialogbrowserwindow-options"
  [window data f]
  (let [document (edn/read-string data)
        file-path (:path document)
        directory (and file-path (.dirname path file-path))
        dialog-options (cond-> dialog-options
                         (and directory (.existsSync fs directory))
                         (assoc :defaultPath directory))]
    (.then (.showSaveDialog dialog window (clj->js dialog-options))
           (fn [^js file]
             (when-not (.-canceled file)
               (write-file (.-filePath file) document f))))))

(defn save
  [window data f]
  (let [document (edn/read-string data)
        file-path (:path document)]
    (if (and file-path (.existsSync fs file-path))
      (write-file file-path document f)
      (save-as window data f))))

(defn open
  "Opens a file.
   https://www.electronjs.org/docs/api/dialog#dialogshowopendialogbrowserwindow-options"
  [window file-path f]
  (if (and file-path (.existsSync fs file-path))
    (read-file file-path f)
    (.then (.showOpenDialog dialog window (clj->js dialog-options))
           (fn [^js/Object file]
             (when-not (.-canceled file)
               (let [file-path (first (js->clj (.-filePaths file)))]
                 (read-file file-path f)))))))

(def export-options
  {:defaultPath (.getPath app "pictures")
   :filters [{:name "svg"
              :extensions ["svg" "svgo"]}]})

(defn export
  "Exports the provided data."
  [window data]
  (.then (.showSaveDialog dialog window (clj->js export-options))
         (fn [^js/Object file]
           (when-not (.-canceled file)
             (.writeFileSync fs (.-filePath file) data "utf-8")))))
