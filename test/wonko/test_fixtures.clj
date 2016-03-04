(ns wonko.test-fixtures
  (:require  [clojure.test :as t]
             [kits.logging.log-async :as log]
             [wonko.config :as config]
             [wonko.alert :as alert]
             [wonko.kafka.consume :as consume]
             [wonko.export.prometheus :as prometheus]))

(defn with-cleared-prometheus-state
  "Clear all state related to Prometheus export,
   such as registries and metrics."
  [test-fn]
  (swap! prometheus/created-metrics (constantly {}))
  (test-fn))

(defn init-consumption [test-fn]
  (log/start-thread-pool! (config/lookup :log))
  (alert/init! (config/lookup :alert-thread-pool-size))
  (consume/init! (config/lookup :kafka :consumer))
  (test-fn)
  (alert/deinit!))
