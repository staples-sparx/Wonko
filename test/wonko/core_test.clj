(ns wonko.core-test
  (:require [clojure.test :refer :all]
            [kits.logging.log-async :as log]
            [wonko.alert :as alert]
            [wonko-client.core :as client]
            [wonko-client.kafka-producer :as client-kp]
            [wonko.config :as config]
            [wonko.core :refer :all]
            [wonko.export.prometheus :as prometheus]
            [wonko.kafka.admin :as admin]
            [wonko.kafka.consume :as consume]
            [wonko.test-utils :as tu]))

(def kafka-config
  {"bootstrap.servers" "127.0.0.1:9092"
   "compression.type" "gzip"
   "linger.ms" 5})

(defn init-consumption [test-fn]
  (log/start-thread-pool! (config/lookup :log))
  (alert/init! (config/lookup :alert-thread-pool-size))
  (consume/init! (config/lookup :kafka :consumer))
  (test-fn)
  (alert/deinit!))

(use-fixtures :each init-consumption)

(defn gen-events []
  (client/counter :found-sku-with-negative-min-value nil)
  (client/counter :defensive/compute {:status :start})
  (client/counter :cogs/job {:status :feed-unavailable} :alert true)

  (client/gauge :current-temperature nil 107)
  (client/gauge :cogs-job-stats {:type :success} 107)
  (client/gauge :skus-job-stats {:type :errors} 107 :alert true))

(defn create-topic-and-gen-events [topic service-name]
  ;; create topic
  (admin/create-topic topic)
  (client/init! service-name kafka-config)
  (client-kp/change-topic! topic)

  ;; produce
  (gen-events))

(deftest test-production-and-consumption
  (testing "that production and consumption of counters and gauges works"
    (let [topic (tu/rand-topic-name)]
      (create-topic-and-gen-events topic "wonko-test-service")
      (let [consumed-events (atom [])
            thread-pool (consume/start {topic 1} #(swap! consumed-events conj %))]
        (tu/wait-for #(= 6 (count @consumed-events)) :interval 1 :timeout 3)
        (is (= (count @consumed-events) 6))
        (is (= #{:found-sku-with-negative-min-value
                 :defensive/compute
                 :cogs/job
                 :current-temperature
                 :cogs-job-stats
                 :skus-job-stats}
               (set (map keyword (map :metric-name @consumed-events)))))
        (consume/stop thread-pool)))))

(deftest test-alerts
  (testing "that alerts work"
    (let [topic (tu/rand-topic-name)
          service-name "wonko-test-service"]
      (create-topic-and-gen-events topic service-name)
      (let [alert-requests (atom [])
            alert-config (assoc-in (config/lookup :pager-duty)
                                   [:api-keys service-name]
                                   "test-api-key")
            process-fn (fn [event]
                         (alert/pager-duty alert-config event)
                         (prometheus/register-event event))]
        (with-redefs [clj-http.client/post
                      (fn [url req]
                        (swap! alert-requests conj req)
                        {:status 200 :headers {} :body ""})]
          (let [thread-pool (consume/start {topic 1} process-fn)]
            (tu/wait-for #(= 2 (count @alert-requests)) :interval 1 :timeout 10)
            (is (= (count @alert-requests) 2))
            (consume/stop thread-pool)))))))

(deftest test-prometheus-export
  (testing "that prometheus export works"
    (let [topic (tu/rand-topic-name)
          service-name "wonko-test-export-service"]
      (create-topic-and-gen-events topic "wonko-test-export-service")
      (let [consumed-events (atom [])
            thread-pool (consume/start {topic 1} (fn [event]
                                                   (process event)
                                                   (swap! consumed-events conj event)))]
        (tu/wait-for #(= 6 (count @consumed-events)) :interval 1 :timeout 3)
        (is (= (count @consumed-events) 6))
        (is (= {"defensive_compute" 1.0
                "found_sku_with_negative_min_value" 1.0
                "current_temperature" 107.0
                "cogs_job" 1.0
                "skus_job_stats" 107.0
                "cogs_job_stats" 107.0}
               (->> [service-name :registry]
                    (get-in @prometheus/created-metrics)
                    tu/prometheus-registry->map)))
        (consume/stop thread-pool)))))
