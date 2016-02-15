(ns wonko.spike.event-source
  (:require [wonko.spike.kafka.produce :as p]
            [wonko.spike.admin :as admin])
  (:import [org.apache.kafka.clients.producer Producer]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; This namespace emulates krikkit, uses the wonko producer to send ;;
;; monitoring events to wonko.                                      ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce stop? (atom false))

(defn stop []
  (swap! stop? (constantly true)))

(defn run [event-gen-fn]
  ;; Very stupid function to keep producing events in the background
  (swap! stop? (constantly false))
  (future
    (loop []
      (event-gen-fn)
      (when-not @stop?
        (Thread/sleep 1000)
        (recur)))))

(defn krikkit []
  (let [topic "krikkit"
        ^Producer producer (p/create-producer)]
    (admin/create-topic topic)
    (run
      (fn []
        (spit "wonko.log" "generating krikkit events\n" :append true)
        (p/counter producer topic :cogs-job-completed)
        (p/counter producer topic :no-new-surise-feed-found :alert true)

        ;; events of this form are not supported currently
        ;; (p/gauge producer topic :cogs-job-stats {:successes 107 :errors 3 :exec-time 42})


        (p/gauge producer topic :cogs-job-stats-successes 107)
        (p/gauge producer topic :cogs-job-stats-errors 3)
        (p/gauge producer topic :cogs-job-stats-exec-time 42)))))

(defn eccentrica []
  (let [topic "eccentrica"
        ^Producer producer (p/create-producer)]
    (admin/create-topic topic)
    (run
      (fn []
        (p/counter producer topic :get-buckets-200)
        (p/counter producer topic :get-buckets-400)
        (p/counter producer topic :get-user-token-200)
        (p/counter producer topic :get-user-token-400)
        (p/gauge producer topic :get-buckets-exec-time 10)
        (p/gauge producer topic :get-user-token-exec-time 15)
        (p/counter producer topic :no-current-experiment :alert true)))))
