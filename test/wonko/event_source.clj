(ns wonko.event-source
  (:require [kits.logging.log-async :as log]
            [wonko-client.core :as client]
            [wonko.kafka.admin :as admin])
  (:import org.apache.kafka.clients.producer.Producer))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; This namespace emulates krikkit, uses the wonko producer to send ;;
;; monitoring events to wonko.                                      ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce stop? (atom false))

(defn stop []
  (swap! stop? (constantly true)))

(def kafka-config
  {"bootstrap.servers" "127.0.0.1:9092"
   "compression.type" "gzip"
   "linger.ms" 5})

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
  (client/init! "krikkit" kafka-config)
  (run
    (fn []
      (log/info {:ns :event-source :msg "generating krikkit events"})
      (client/counter :found-sku-with-negative-min-value nil)
      (client/counter :defensive/compute {:status :start})
      (client/counter :defensive/compute {:status :done})
      (client/counter :cogs/job {:status :feed-unavailable})

      (client/alert :s3-down {:msg "Couldn't reach S3 for something."})

      (client/gauge :cogs-job-stats {:type :success} (rand-int 500))
      (client/gauge :cogs-job-stats {:type :errors} (rand-int 500))
      (client/gauge :cogs-job-stats {:type :exec-time} (rand-int 500))

      (client/stream :feed-length {:feed :cogs} (rand-int 1000))
      (client/stream :feed-length {:feed :sku} (rand-int 1000)))))

(defn maybe-do
  "Calls f percentage times out of 100 times it's called.
  Used to create realistic scenarios where confirm percentage is
  about 3% and puchase percentage is about 1%"
  [percentage f]
  (when (< (rand-int 100) percentage)
    (f)))

(defn eccentrica []
  (client/init! "eccentrica" kafka-config)
  (run
    (fn []
      (log/info {:ns :event-source :msg "generating eccentrica events"})

      (client/stream :get-user-token {:status 200} (rand-int 10))
      (maybe-do 10 #(client/stream :get-user-token {:status 400} (rand-int 10)))

      (client/stream :get-buckets {:status 200} (rand-int 10))
      (maybe-do 10 #(client/stream :get-buckets {:status 400} (rand-int 10)))

      (client/stream :confirm-bucket {:status 200} (rand-int 10))
      (maybe-do 10 #(client/stream :confirm-bucket {:status 400} (rand-int 10)))

      (maybe-do 1 #(client/alert :no-current-experiment {:exp-name (str "exp-" (rand-int 5))})))))
