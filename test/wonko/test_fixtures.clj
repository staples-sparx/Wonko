(ns wonko.test-fixtures
  (:require  [clojure.test :as t]
             [wonko.export.prometheus :as prometheus]))

(defn with-cleared-prometheus-state
  "Clear all state related to Prometheus export,
   such as registries and metrics."
  [test-fn]
  (swap! prometheus/created-metrics (constantly {}))
  (test-fn))
