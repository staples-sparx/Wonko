(ns wonko.test-fixtures
  (:require [kits.logging.log-async :as log]
            [wonko
             [alert :as alert]
             [config :as config]
             [test-config :as tc]
             [test-utils :as tu]]
            [wonko-client.core :as client]
            [wonko.export.prometheus :as prometheus]
            [wonko.kafka.consume :as consume]))

(defn with-cleared-prometheus-state
  "Clear all state related to Prometheus export,
   such as registries and metrics."
  [test-fn]
  (swap! prometheus/created-metrics (constantly {}))
  (test-fn))


(defn with-initialized-client
  "Initialize `wonko-client'. Topics still need to be
   set manually."
  [test-fn]
  (client/init! (tu/rand-str "test-service") tc/kafka-config)
  (test-fn))

(defn with-initialized-consumption [test-fn]
  (log/start-thread-pool! (config/lookup :log))
  (alert/init! (config/lookup :alert-thread-pool-size))
  (consume/init! (config/lookup :kafka :consumer))
  (test-fn)
  (alert/deinit!))
