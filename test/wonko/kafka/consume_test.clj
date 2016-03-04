(ns wonko.kafka.consume-test
  (:require [clojure.test :refer :all]
            [wonko.kafka.consume :as sut]
            [wonko.test-utils :as tu]
            [wonko-client.core :as client]
            [wonko.test-config :as tc]
            [wonko.test-fixtures :as tf]
            [wonko.kafka.admin :as admin]))

(use-fixtures :each tf/with-initialized-client)

(deftest consumption-offset
  (testing "after a restart, don't start consuming from the beginning"
    (sut/init! tc/zookeeper-config)

    (let [topic (tu/rand-str "offset-test")
          events (atom [])
          process-fn #(swap! events conj %)
          thread-pool (sut/start {topic 1} process-fn)]

      (admin/create-topic topic)
      (client/set-topics! topic topic)

      ;; First Run
      (is (client/counter :first-metric nil))

      (tu/wait-for #(= 1 (count @events)) :interval 0.5 :timeout 5)
      (is (= 1 (count @events)))

      (is (= "counter" (-> @events first :metric-type)))
      (is (= "first-metric" (-> @events first :metric-name)))

      ;; Restart
      (sut/stop thread-pool)
      (sut/init! tc/zookeeper-config)
      (reset! events [])

      ;; Second Run
      (let [thread-pool (sut/start {topic 1} process-fn)]
        (is (client/gauge :second-metric nil 5))

        (tu/wait-for #(= 1 (count @events)) :interval 0.5 :timeout 2)
        (is (= 1 (count @events)))

        (is (= "gauge" (-> @events first :metric-type)))
        (is (= "second-metric" (-> @events first :metric-name)))

        (sut/stop thread-pool)))))
