(ns reply.eval-modes.standalone
  (:require [reply.eval-modes.standalone.concurrency :as concurrency]
            [reply.eval-state :as eval-state]
            [reply.initialization :as initialization]
            [reply.reader.jline :as jline]
            [reply.reader.simple-jline :as simple-jline]
            [reply.signals :as signals]))

(defn reply-read [options]
  (fn [prompt exit]
    (concurrency/starting-read!)
    (binding [*ns* (eval-state/get-ns)]
      (let [result (jline/read prompt exit options)]
        (when-let [reader @jline/jline-reader]
          (simple-jline/prepare-for-next-read reader)
          (simple-jline/shutdown {:reader reader})
          (reset! jline/jline-reader nil)
          (reset! jline/jline-pushback-reader nil))
        result))))

(def reply-eval
  (concurrency/act-in-future
    (fn [form]
      (eval-state/with-bindings
        (partial eval form)))))

(def reply-print
  (concurrency/act-in-future prn))

(defn handle-ctrl-c [signal]
  (concurrency/stop-running-actions))

(defn main [options]
  (signals/set-signal-handler! "INT" handle-ctrl-c)
  (eval (initialization/construct-init-code options))
  (clojure.main/repl :read (reply-read options)
                     :eval reply-eval
                     :print reply-print
                     :prompt (constantly false)
                     :need-prompt (constantly false)))

