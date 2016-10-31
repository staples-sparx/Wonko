(ns wonko.export.prometheus.register
  (:require [clojure.string :as s]
            [wonko.constants :as c])
  (:import [io.prometheus.client Gauge Counter Histogram]))

(defn- maybe-set-label-values [metric label-values]
  (if (seq label-values)
    (.labels metric (into-array (mapv str label-values)))
    metric))

(defn- histogram [metric label-values metric-value]
  (-> metric
      (maybe-set-label-values label-values)
      (.observe (double metric-value))))

(defn- summary [metric label-values metric-value]
  (-> metric
      (maybe-set-label-values label-values)
      (.observe (double metric-value))))

(defn- gauge [metric label-values metric-value]
  (-> metric
      (maybe-set-label-values label-values)
      (.set (double metric-value))))

(defn- counter [metric label-values]
  (-> metric
      (maybe-set-label-values label-values)
      .inc))

(defn- stream [metric label-values metric-value]
  {c/histogram (histogram (get metric c/histogram) label-values metric-value)
   c/summary (summary (get metric c/summary) label-values metric-value)})

(defn metric [metric {:keys [label-values metric-value metric-type] :as event}]
  (condp = metric-type
    c/counter (counter metric label-values)
    c/gauge (gauge metric label-values metric-value)
    c/stream (stream metric label-values metric-value)))
