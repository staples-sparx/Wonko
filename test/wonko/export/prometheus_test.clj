(ns wonko.export.prometheus-test
  (:require [wonko.export.prometheus :as sut]
            [wonko.test-fixtures :as tf]
            [clojure.test :refer :all])
  (:import [java.util.concurrent Executors TimeUnit]))

(use-fixtures :each tf/with-cleared-prometheus-state)

(deftest thread-safety
  (testing "get-or-create-registry is thread-safe"
    (let [registries (atom #{})
          thread-pool (Executors/newFixedThreadPool 10)
          process-fn #(swap! registries conj (sut/get-or-create-registry "registry-test-service"))]

      (dorun (for [i (range 99999)] (.submit thread-pool ^Runnable process-fn)))
      (.shutdown thread-pool)
      (.awaitTermination thread-pool Integer/MAX_VALUE TimeUnit/MILLISECONDS)

      (is (= 1 (count @registries)))))

  (testing "get-or-create-metric is thread-safe"
    (let [registry (sut/get-or-create-registry "metric-test-service")
          metrics (atom #{})
          thread-pool (Executors/newFixedThreadPool 10)
          metric-attrs {:metric-name "metric" :metric-type "counter"}
          process-fn #(swap! metrics conj (sut/get-or-create-metric registry metric-attrs))]

      (dorun (for [i (range 99999)] (.submit thread-pool ^Runnable process-fn)))
      (.shutdown thread-pool)
      (.awaitTermination thread-pool Integer/MAX_VALUE TimeUnit/MILLISECONDS)

      (is (= 1 (count @metrics))))))
