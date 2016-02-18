(ns wonko.utils
  (:import [java.util.concurrent Executors]))

(defn create-thread-pool [num-threads]
  (Executors/newFixedThreadPool num-threads))
