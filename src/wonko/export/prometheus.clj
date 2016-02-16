(ns wonko.export.prometheus
  (:require [clojure.string :as s]
            [wonko.export.prometheus.create :as create]
            [wonko.export.prometheus.register :as register])
  (:import [io.prometheus.client Gauge Counter Histogram]
           [io.prometheus.client.hotspot DefaultExports]
           [io.prometheus.client CollectorRegistry]
           [io.prometheus.client.exporter.common TextFormat]
           [java.io StringWriter]))

;; This contains prometheus created metrics in a map of the form:
;; {:topic {:metric-type {metric-name metric} :registry registry}}
(defonce created-metrics
  (atom {}))

(defn get-label-names [properties]
  (sort (keys properties)))

(defn get-label-values [properties]
  (map properties (get-label-names properties)))

(defn get-or-create-metric [registry topic {:keys [metric-name metric-type properties] :as event}]
  (let [metric-path [topic (keyword metric-type) metric-name]
        label-names (get-label-names properties)]
    (or (get-in @created-metrics metric-path)
        (let [created-metric (create/metric registry (assoc event :label-names label-names))]
          (swap! created-metrics assoc-in metric-path created-metric)
          created-metric))))

(defn get-or-create-registry [topic]
  (let [registry-path [topic :registry]]
    (or (get-in @created-metrics registry-path)
        (let [created-registry (create/registry)]
          (swap! created-metrics assoc-in registry-path created-registry)
          created-registry))))

(defn register-event [topic {:keys [metric-value properties] :as event}]
  (let [registry (get-or-create-registry topic)
        metric (get-or-create-metric registry topic event)
        label-values (get-label-values properties)]
    (register/metric metric (assoc event :label-values label-values))))

(defn metrics-endpoint [topic]
  (let [registry (get-or-create-registry topic)
        writer (StringWriter.)]
    (TextFormat/write004 writer (.metricFamilySamples registry))
    {:status  200
     :headers {"Content-Type" TextFormat/CONTENT_TYPE_004}
     :body    (.toString writer)}))
