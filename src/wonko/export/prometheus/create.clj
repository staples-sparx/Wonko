(ns wonko.export.prometheus.create
  (:require [clojure.string :as s]
            [wonko.constants :as c])
  (:import [io.prometheus.client Gauge Counter Summary Histogram]
           [io.prometheus.client CollectorRegistry]))

(defn registry []
  (CollectorRegistry.))

(defn clear-registry [registry]
  (.clear registry))

(defn ->prometheus-name [metric-name]
  (-> metric-name
      name
      (s/replace #"[-/ ]" "_")))

(defn- maybe-set-label-names [metric label-names]
  (if (seq label-names)
    (.labelNames metric (into-array (mapv ->prometheus-name label-names)))
    metric))

(defn- set-basics [metric metric-name help label-names]
  (-> metric
      (.name (->prometheus-name metric-name))
      (maybe-set-label-names label-names)
      (.help help)))

(defn- counter [registry metric-name help label-names]
  (-> (Counter/build)
      (set-basics metric-name help label-names)
      (.register registry)))

(defn- gauge [registry metric-name help label-names]
  (-> (Gauge/build)
      (set-basics metric-name help label-names)
      (.register registry)))

(defn- summary [registry metric-name help label-names]
  (-> (Summary/build)
      (set-basics metric-name help label-names)
      (.register registry)))

(defn- histogram [registry metric-name help label-names
                 {:keys [start width count] :or {start 0} :as bucket-config}]
  (-> (Histogram/build)
      (set-basics metric-name help label-names)
      (.linearBuckets (double start) (double width) (int count))
      (.register registry)))

(defn- stream [registry metric-name help-text label-names options]
  (let [h-metric-name (str metric-name "-" c/histogram)
        s-metric-name (str metric-name "-" c/summary)
        bucket-info {:start (or (:bucket-start options) 0)
                     :width (or (:bucket-width options) 1)
                     :count (or (:bucket-count options) 30)}]
    {c/histogram (histogram registry h-metric-name help-text label-names
                            bucket-info)}))

(defn metric
  [registry {:keys [metric-name metric-type label-names options] :as event}]
  (condp = metric-type
    c/counter (counter registry metric-name "help-text" label-names)
    c/gauge (gauge registry metric-name "help-text" label-names)
    c/stream (stream registry metric-name "help-text" label-names options)))
