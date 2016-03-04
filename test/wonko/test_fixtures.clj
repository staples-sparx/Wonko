(ns wonko.test-fixtures
  (:require [wonko
             [test-config :as tc]
             [test-utils :as tu]]
            [wonko-client.core :as client]
            [wonko.export.prometheus :as prometheus]))

(defn with-cleared-prometheus-state
  "Clear all state related to Prometheus export,
   such as registries and metrics."
  [test-fn]
  (swap! prometheus/created-metrics (constantly {}))
  (test-fn))

(defn with-initialized-client
  [test-fn]
  (client/init! (tu/rand-str "test-service") tc/kafka-config))
