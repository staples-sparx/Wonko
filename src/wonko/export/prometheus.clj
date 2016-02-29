(ns wonko.export.prometheus
  (:require [clojure.string :as s]
            [kits.logging.log-async :as log]
            [ring.util.response :as res]
            [wonko.export.prometheus
             [create :as create]
             [register :as register]]
            [wonko.test-utils :as tu])
  (:import [io.prometheus.client CollectorRegistry Counter Gauge Histogram]
           io.prometheus.client.exporter.common.TextFormat
           io.prometheus.client.hotspot.DefaultExports
           java.util.concurrent.locks.ReentrantLock
           java.io.StringWriter))

;; This contains prometheus created metrics in a map of the form:
;; {:service {:metric-type {metric-name metric} :registry registry}}
(defonce created-metrics
  (atom {}))

(defonce lock (ReentrantLock.))

(defn get-label-names [properties]
  (sort (keys properties)))

(defn get-label-values [properties]
  (map properties (get-label-names properties)))

(defn get-or-create-metric [registry {:keys [service metric-name metric-type properties] :as event}]
  (let [metric-path [service (keyword metric-type) metric-name]
        label-names (get-label-names properties)]
    (or (get-in @created-metrics metric-path)
        (let [created-metric (create/metric registry (assoc event :label-names label-names))]
          (swap! created-metrics assoc-in metric-path created-metric)
          created-metric))))

(defn get-or-create-registry [service]
  (let [registry-path [service :registry]]
    (or (get-in @created-metrics registry-path)
        (let [created-registry (create/registry)]
          (swap! created-metrics assoc-in registry-path created-registry)
          created-registry))))

(defn register-event [{:keys [service metric-value properties] :as event}]
  ;; FIXME: This is a broad fix to make this function thread-safe. We should
  ;; understand the problem better and apply the fix in a more localized way.
  (try
    (let [registry (get-or-create-registry service)
          metric (get-or-create-metric registry event)
          label-values (get-label-values properties)]
      (register/metric metric (assoc event :label-values label-values)))
    (catch Exception e
      (log/info {:msg "unable to register event in prometheus" :event event}))))

(defn metrics-endpoint [service]
  (let [registry (get-in @created-metrics [service :registry])
        writer (StringWriter.)]
    (if registry
      (do (TextFormat/write004 writer (.metricFamilySamples registry))
          {:status  200
           :headers {"Content-Type" TextFormat/CONTENT_TYPE_004}
           :body    (.toString writer)})
      (res/not-found "Not found"))))
