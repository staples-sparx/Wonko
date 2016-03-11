(ns wonko.export.prometheus-test
  (:require [wonko.export.prometheus :as prom]
            [wonko.test-fixtures :as tf]
            [clojure.test :refer :all])
  (:import [java.util.concurrent Executors TimeUnit]))

(use-fixtures :each tf/with-cleared-prometheus-state)

(deftest label-names
  (testing "an exception is thrown when label names are changed for a metric"
    (let [registry (prom/get-or-create-registry "label-names-test-service")]
      (prom/register-event {:service "label-names-test-service" :metric-name "metric" :metric-type "counter"
                           :properties {:foo "bar"}})

      (is (thrown-with-msg?
           IllegalArgumentException #"Incorrect number of labels"
           (prom/register-event {:service "label-names-test-service" :metric-name "metric" :metric-type "counter"
                                :properties {:foo "bar" :baz "quux"}}))))))

(deftest thread-safety
  (testing "get-or-create-registry is thread-safe"
    (let [registries (atom #{})
          thread-pool (Executors/newFixedThreadPool 10)
          process-fn #(swap! registries conj (prom/get-or-create-registry "registry-test-service"))]

      (dorun (for [i (range 99999)] (.submit thread-pool ^Runnable process-fn)))
      (.shutdown thread-pool)
      (.awaitTermination thread-pool Integer/MAX_VALUE TimeUnit/MILLISECONDS)

      (is (= 1 (count @registries)))))

  (testing "get-or-create-metric is thread-safe"
    (let [registry (prom/get-or-create-registry "metric-test-service")
          metrics (atom #{})
          thread-pool (Executors/newFixedThreadPool 10)
          metric-attrs {:metric-name "metric" :metric-type "counter"}
          process-fn #(swap! metrics conj (prom/get-or-create-metric registry metric-attrs))]

      (dorun (for [i (range 99999)] (.submit thread-pool ^Runnable process-fn)))
      (.shutdown thread-pool)
      (.awaitTermination thread-pool Integer/MAX_VALUE TimeUnit/MILLISECONDS)

      (is (= 1 (count @metrics))))))

(deftest test-clear-service-data
  (testing "clearing service data resets the service's metrics in the
            atom and deletes time series from prometheus"
    (let [service-name "test-clear-service"
          registry (prom/get-or-create-registry service-name)
          received-request? (atom false)]
      (prom/register-event {:service service-name
                            :metric-name "metric"
                            :metric-type "counter"
                           :properties {:foo "bar"}})
      (is (boolean (get-in @prom/created-metrics [service-name :registry])))
      (is (boolean (get-in @prom/created-metrics [service-name :counter "metric"])))

      (with-redefs [clj-http.client/delete (fn [url req]
                                             (swap! received-request? (constantly true)))]
        (prom/clear-service-data! "localhost:9090" service-name))

      (is (= {} (get @prom/created-metrics service-name)))
      (is (true? @received-request?)))))
