(ns wonko.export.prometheus.register
  (:require [clojure.string :as s])
  (:import [io.prometheus.client Gauge Counter Histogram]))

(defn maybe-set-label-values [metric label-values]
  (if (seq label-values)
    (.labels metric (into-array (mapv str label-values)))
    metric))

(defn histogram [metric label-values metric-value]
  (-> metric
      (maybe-set-label-values label-values)
      (.observe (double metric-value))))

(defn summary [metric label-values metric-value]
  (-> metric
      (maybe-set-label-values label-values)
      (.observe (double metric-value))))

(defn stream [metric label-values metric-value]
  (-> metric
      (maybe-set-label-values label-values)
      (.observe (double metric-value))))

(defn gauge [metric label-values metric-value]
  (-> metric
      (maybe-set-label-values label-values)
      (.set (double metric-value))))

(defn counter [metric label-values]
  (-> metric
      (maybe-set-label-values label-values)
      .inc))

(defn stream [metric label-values metric-value]
  {"histogram" (histogram (get metric "histogram") label-values metric-value)
   "summary" (summary (get metric "summary") label-values metric-value)})

(defn metric [metric {:keys [label-values metric-value metric-type] :as event}]
  (case metric-type
    "counter" (counter metric label-values)
    "gauge" (gauge metric label-values metric-value)
    "stream" (stream metric label-values metric-value)))
