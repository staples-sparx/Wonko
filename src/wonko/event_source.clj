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

      (client/gauge :cogs-job-stats {:type :success} 107)
      (client/gauge :cogs-job-stats {:type :errors} 107)
      (client/gauge :cogs-job-stats {:type :exec-time} 107))))

(defn eccentrica []
  (client/init! "eccentrica" kafka-config)
  (run
    (fn []
      (log/info {:ns :event-source :msg "generating eccentrica events"})
      (client/counter :get-buckets {:status 200})
      (client/counter :get-buckets {:status 400})
      (client/counter :get-user-token {:status 200})
      (client/counter :get-user-token {:status 400})
      (client/gauge :get-buckets-exec-time {:status 200} 10)
      (client/gauge :get-user-token-exec-time {:status 200} 15)
      (client/counter :no-current-experiment {}))))
