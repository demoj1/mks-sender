(ns main
  (:gen-class)
  (:require [clojure.string :as str]
            [clj-http.client :as client]
            [taoensso.timbre :as l]
            [clojure.java.io :as io])
  (:import (java.net Socket SocketTimeoutException)
           (java.io DataInputStream DataOutputStream FileWriter File)))

(def ^:private file-separator (File/separator))
(def ^:private ^String tmp-file
  (.getAbsolutePath
    (io/file (System/getProperty "java.io.tmpdir") "mks-sender.log")))
(def ^:private socket (atom nil))
(def ^:private in (atom nil))
(def ^:private out (atom nil))

(defn- <-socket
  "Read message from socket"
  []

  (let [result
        (->>
          (iterate
            (fn [acc]
              (try
                (conj acc (.readLine @in))
                (catch SocketTimeoutException _e (conj acc :EOF))))
            [])
          (take-while #(not= (last %) :EOF))
          (last))]
    (l/info result)
    result))

(defn- ok?
  "Assertion reaction on Gcode command"
  [pattern]

  (let [receive (<-socket)]
    (l/info (str "Expecting: " pattern " | Receive: " receive))
    (assert (= pattern receive))
    (l/info "Assertion success")))

(defn- ->socket
  "Write msg to socket"
  ([msg]
   (l/info (str "Write Gcode: " msg))
   (.write @out (.getBytes (str msg "\r\n"))))
  ([msg pattern]
   (->socket msg)
   (ok? pattern)))

(defn- start-print [filename]
  (->socket (str "M23 " filename) ["File selected" "ok"])
  (->socket "M24 "))

(defn- get-last-component [filename] (-> filename (str/split (re-pattern file-separator)) (last)))

(defn- load-file-to-printer
  "HTTP upload file to printer"
  [path ip]

  (let [last-component (get-last-component path)
        url (str "http://" ip "/upload?X-Filename=" last-component)]
    (->socket (str "M30 " last-component))
    (l/info (<-socket))
    (l/info "Start uploading file")
    (client/post url {:body (clojure.java.io/file path)
                      :content-type "application/octet-stream"})))

(defn -main [^String ip ^String path]
  (binding [*out* (FileWriter. tmp-file)]
    (let [last-component (get-last-component path)]
      (l/info (str "-------" " Start print file: " path))
      (reset! socket (Socket. ip 8080))
      (.setSoTimeout @socket 500)
      (reset! in (DataInputStream. (.getInputStream @socket)))
      (reset! out (DataOutputStream. (.getOutputStream @socket)))
      (load-file-to-printer path ip)
      (->socket "M20")
      (l/info (<-socket))
      (start-print last-component))))