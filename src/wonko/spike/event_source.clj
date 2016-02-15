(ns wonko.spike.event-source
  (:require [wonko.spike.kafka.produce :as p]
            [wonko.spike.kafka.admin :as admin])
  (:import [org.apache.kafka.clients.producer Producer]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; This namespace emulates krikkit, uses the wonko producer to send ;;
;; monitoring events to wonko.                                      ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce stop? (atom false))

(defn stop []
  (swap! stop? (constantly true)))

(defn config [topic]
  {:topic topic
   :producer {"bootstrap.servers" "127.0.0.1:9092"
              "compression.type" "gzip"
              "linger.ms" 5}})

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
  (let [topic "krikkit"]
    (p/init! (config topic))
    (admin/create-topic topic)
    (run
      (fn []
        (spit "wonko.log" "generating krikkit events\n" :append true)
        (p/counter :found-sku-with-negative-min-value nil)
        (p/counter :defensive/compute {:status :start})
        (p/counter :defensive/compute {:status :done})
        (p/counter :cogs/job {:status :feed-unavailable})

        ;; events of this form are not supported currently
        ;; (p/gauge :cogs-job-stats {:successes 107 :errors 3 :exec-time 42})

        (p/gauge :cogs-job-stats {:type :success} 107)
        (p/gauge :cogs-job-stats {:type :errors} 107)
        (p/gauge :cogs-job-stats {:type :exec-time} 107)))))

(defn eccentrica []
  (let [topic "eccentrica"
        ^Producer producer (p/create-producer)]
    (admin/create-topic topic)
    (run
      (fn []
        (p/counter :get-buckets-200)
        (p/counter :get-buckets-400)
        (p/counter :get-user-token-200)
        (p/counter :get-user-token-400)
        (p/gauge :get-buckets-exec-time 10)
        (p/gauge :get-user-token-exec-time 15)
        (p/counter :no-current-experiment :alert true)))))
