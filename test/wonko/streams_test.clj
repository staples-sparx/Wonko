(ns wonko.streams-test
  (:require [clojure.test :refer :all]
            [wonko.core :as core]
            [wonko-client.core :as client]
            [wonko.export.prometheus :as prometheus]
            [wonko.kafka.consume :as consume]
            [wonko.test-fixtures :as tf]
            [wonko.test-utils :as tu]))

(use-fixtures :each tf/with-initialized-consumption)

(deftest test-streams
  (testing "producing, consuming and exporting streams works"
    (let [service-name (str (gensym "wonko-test-streams-service"))
          {:keys [events-topic alerts-topic]} (tu/create-topics)
          consumed-events (atom [])
          thread-pool (consume/start {events-topic 1 alerts-topic 1}
                                     (fn [event]
                                       (core/process event)
                                       (swap! consumed-events conj event)))]
      (tu/init-client service-name events-topic alerts-topic)
      (client/stream :feed-length
                     {:status :success}
                     0
                     :bucket-width 5
                     :bucket-count 20)
      (client/stream :feed-length
                     {:status :success}
                     10
                     :bucket-width 5
                     :bucket-count 20)
      (client/stream :feed-length
                     {:status :success}
                     20
                     :bucket-width 5
                     :bucket-count 20)
      (tu/wait-for #(= 3 (count @consumed-events)) :interval 1 :timeout 3)

      (is (= 3 (count @consumed-events)))

      (let [first-metric (first (filter #(= 0 (:metric-value %)) @consumed-events))]
        (is (= (dissoc first-metric :metadata)
               {:service service-name
                :metric-name "feed-length"
                :metric-type "stream"
                :metric-value 0
                :properties {:status "success"}
                :options {:bucket-width 5 :bucket-count 20}})))

      (let [histogram (get-in @prometheus/created-metrics
                              [service-name :stream "feed-length" "histogram"])
            summary (get-in @prometheus/created-metrics
                            [service-name :stream "feed-length" "summary"])]

        (is (boolean histogram))
        (is (boolean summary))

        (testing "histograms"
          (let [res (tu/find-metric-sample histogram "feed_length_histogram_sum")]
            (is (= (:label-names res) ["host" "ip_address" "status"]))
            (is (= (nth (:label-values res) 2) "success"))
            (is (= (:name res) "feed_length_histogram_sum"))
            (is (= (:value res) 30.0)))

          (let [res (tu/find-metric-sample histogram "feed_length_histogram_count")]
            (is (= (:label-names res) ["host" "ip_address" "status"]))
            (is (= (nth (:label-values res) 2) "success"))
            (is (= (:name res) "feed_length_histogram_count"))
            (is (= (:value res) 3.0))))

        (testing "summaries"
          (let [[f s] (tu/metric-samples summary)]
            (is (= ["host" "ip_address" "status"]
                   (:label-names f)
                   (:label-names s)))
            (is (= "success"
                   (nth (:label-values f) 2)
                   (nth (:label-values s) 2)))
            (is (= (:name f) "feed_length_summary_count"))
            (is (= (:value f) 3.0))
            (is (= (:name s) "feed_length_summary_sum"))
            (is (= (:value s) 30.0)))))

      (consume/stop thread-pool)
      (tu/delete-topics events-topic alerts-topic))))
