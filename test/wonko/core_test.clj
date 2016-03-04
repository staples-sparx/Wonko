(ns wonko.core-test
  (:require [clojure.test :refer :all]
            [wonko.alert :as alert]
            [wonko-client.core :as client]
            [wonko.config :as config]
            [wonko.core :refer :all]
            [wonko.export.prometheus :as prometheus]
            [wonko.kafka.consume :as consume]
            [wonko.test-utils :as tu]
            [wonko.test-fixtures :as tf]))

(use-fixtures :each tf/with-initialized-consumption)

(defn gen-events [service-name events-topic alerts-topic]
  (tu/init-client service-name events-topic alerts-topic)

  (client/counter :found-sku-with-negative-min-value nil)
  (client/counter :defensive/compute {:status :start})
  (client/counter :cogs/job {:status :feed-unavailable})

  (client/alert :cogs-feed-unavailable {:msg "do something about this fast."})

  (client/gauge :current-temperature nil 107)
  (client/gauge :cogs-job-stats {:type :success} 107)
  (client/gauge :skus-job-stats {:type :errors} 107))

(deftest test-production-and-consumption
  (testing "that production and consumption of counters and gauges works"
    (let [service-name  "wonko-test-service"
          {:keys [events-topic alerts-topic]} (tu/create-topics)
          consumed-events (atom [])
          thread-pool (consume/start {events-topic 1 alerts-topic 1}
                                     #(swap! consumed-events conj %))]
      (gen-events service-name events-topic alerts-topic)
      (tu/wait-for #(= 7 (count @consumed-events)) :interval 1 :timeout 3)
      (is (= (count @consumed-events) 7))
      (is (= #{:found-sku-with-negative-min-value
               :defensive/compute
               :cogs/job
               :cogs-feed-unavailable
               :current-temperature
               :cogs-job-stats
               :skus-job-stats}
             (set (map keyword (map :metric-name @consumed-events)))))
      (consume/stop thread-pool)
      (tu/delete-topics events-topic alerts-topic))))

(deftest test-alerts
  (testing "that alerts work"
    (let [service-name "wonko-test-service"
          {:keys [events-topic alerts-topic]} (tu/create-topics)]
      (gen-events service-name events-topic alerts-topic)
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
          (let [thread-pool (consume/start {events-topic 1 alerts-topic 1} process-fn)]
            (tu/wait-for #(= 2 (count @alert-requests)) :interval 1 :timeout 10)
            (is (= (count @alert-requests) 1))
            (consume/stop thread-pool)
            (tu/delete-topics events-topic alerts-topic)))))))

(deftest test-prometheus-export
  (testing "that prometheus export works"
    (let [service-name "wonko-test-export-service"
          {:keys [events-topic alerts-topic]} (tu/create-topics)]
      (gen-events service-name events-topic alerts-topic)
      (let [consumed-events (atom [])
            thread-pool (consume/start {events-topic 1 alerts-topic 1}
                                       (fn [event]
                                         (process event)
                                         (swap! consumed-events conj event)))]
        (tu/wait-for #(= 7 (count @consumed-events)) :interval 1 :timeout 3)
        (is (= (count @consumed-events) 7))
        (is (= {"defensive_compute" 1.0
                "found_sku_with_negative_min_value" 1.0
                "current_temperature" 107.0
                "cogs_job" 1.0
                "skus_job_stats" 107.0
                "cogs_job_stats" 107.0
                "cogs_feed_unavailable" 1.0}
               (->> [service-name :registry]
                    (get-in @prometheus/created-metrics)
                    tu/prometheus-registry->map)))
        (consume/stop thread-pool)
        (tu/delete-topics events-topic alerts-topic)))))
