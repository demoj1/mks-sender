(ns main
  (:gen-class)
  (:require [clojure.string :as str]
            [clj-http.client :as client]
            [taoensso.timbre :as l]
            [clojure.java.io :as io])
  (:import (java.net Socket SocketTimeoutException)
           (java.io DataInputStream DataOutputStream FileWriter File)))

(def socket (atom nil))
(def in (atom nil))
(def out (atom nil))

(def ^Integer mks-port 8080)
(def socket-timeout 500)

(def file-separator (File/separator))
(def ^String tmp-file
  (.getAbsolutePath
    (io/file (System/getProperty "java.io.tmpdir") "mks-sender.log")))

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
  [pattern] {:pre [(vector? pattern)]}

  (let [receive (<-socket)]
    (l/info (str "Expecting: " pattern " | Receive: " receive))
    (assert (= pattern receive))
    (l/info "Assertion success")))

(defn- ->socket
  "Write msg to socket"
  ([msg] {:pre [(string? msg)]}

   (l/info (str "Write Gcode: " msg))
   (.write @out (.getBytes (str msg "\r\n"))))

  ([^String msg pattern] {:pre [(string? msg)]}

   (->socket msg)
   (ok? pattern)))

(defn- start-print [filename] {:pre [(string? filename)]}
  (->socket (str "M23 " filename))
  (->socket "M24"))

(defn- get-last-component [filename] {:pre [(string? filename)]}
  (-> filename (str/split (re-pattern file-separator)) (last)))

(defn- load-file-to-printer
  "HTTP upload file to printer"
  [path ip]
  {:pre [(string? path) (string? ip)]}

  (let [last-component (get-last-component path)
        url (str "http://" ip "/upload?X-Filename=" last-component)]
    (->socket (str "M30 " last-component))
    (l/info (<-socket))
    (l/info "Start uploading file")
    (client/post url {:body (clojure.java.io/file path)
                      :content-type "application/octet-stream"})))

(defn trim-comment [^String path-in ^String path-out]
  (with-open [rdr' (io/reader path-in)]
    (loop [r rdr'
           buffer ^StringBuffer (StringBuffer.)]
      (if (.ready r)
        (let [line (.readLine r)
              line' (str/replace line #"( ;.*)|(^;.*)" "")]
          (if (empty? line')
            (recur r buffer)
            (recur r (.append buffer (str line' \newline)))))
        (spit path-out buffer)))))

(defn -main [^String ip ^String path]
  (binding [*out* (FileWriter. tmp-file)]
    (trim-comment path path)
    (let [last-component (get-last-component path)]
      (l/info (str "-------" " Start print file: " path))
      (reset! socket (Socket. ip mks-port))
      (.setSoTimeout @socket socket-timeout)
      (reset! in (DataInputStream. (.getInputStream @socket)))
      (reset! out (DataOutputStream. (.getOutputStream @socket)))
      (load-file-to-printer path ip)
      (->socket "M20")
      (l/info (<-socket))
      (start-print last-component))))