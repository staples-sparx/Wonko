(ns wonko.spike.export.prometheus
  (:require [clojure.string :as s])
  (:import [io.prometheus.client Gauge Counter Histogram]
           [io.prometheus.client.hotspot DefaultExports]
           [io.prometheus.client CollectorRegistry]
           [io.prometheus.client.exporter.common TextFormat]
           [java.io StringWriter]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; This is a bridge to prometheus, and is copied from                   ;;
;; eccentrica.utils.monitoring. We don't use prometheus labels with the ;;
;; current wonko-api/schema.                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ->prometheus-name [metric-name]
  (-> metric-name
      name
      (s/replace #"-" "_")))

(defn set-basics [metric metric-name help label-names]
  (-> metric
      (.name (->prometheus-name metric-name))
      #_(.labelNames (into-array (mapv ->prometheus-name label-names)))
      (.help help)))

(defn create-histogram [metric-name help label-names {:keys [start width count]
                                                      :or {start 0}
                                                      :as bucket-config}]
  (-> (Histogram/build)
      (set-basics metric-name help label-names)
      (.linearBuckets (double start) (double width) (int count))
      .register))

(defn create-counter [metric-name help label-names]
  (-> (Counter/build)
      (set-basics metric-name help label-names)
      .register))

(defn create-gauge [metric-name help label-names]
  (-> (Gauge/build)
      (set-basics metric-name help label-names)
      .register))

(defn registry []
  (CollectorRegistry/defaultRegistry))

(defprotocol Metric
  (register [this label-values value]))

(extend-protocol Metric
  Histogram
  (register [this label-values elapsed-ms]
    (-> this
        #_(.labels (into-array (map str label-values)))
        (.observe (double elapsed-ms))))

  Counter
  (register [this label-values _]
    (-> this
        #_(.labels (into-array (map str label-values)))
        .inc))

  Gauge
  (register [this label-values value]
    (-> this
        #_(.labels (into-array (map str label-values)))
        (.set (double value)))))

(defn metrics-endpoint [request]
  (let [writer (StringWriter.)]
    (TextFormat/write004 writer (.metricFamilySamples (registry)))
    {:status  200
     :headers {"Content-Type" TextFormat/CONTENT_TYPE_004}
     :body    (.toString writer)}))

(defn clear-default-registry []
  (.clear (registry)))

(defn init []
  (DefaultExports/initialize))
