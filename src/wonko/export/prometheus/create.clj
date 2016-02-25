(ns wonko.export.prometheus.create
  (:require [clojure.string :as s])
  (:import [io.prometheus.client Gauge Counter Histogram]
           [io.prometheus.client CollectorRegistry]))

(defn registry []
  (CollectorRegistry.))

(defn clear-registry [registry]
  (.clear registry))

(defn ->prometheus-name [metric-name]
  (-> metric-name
      name
      (s/replace #"[-/ ]" "_")))

(defn maybe-set-label-names [metric label-names]
  (if (seq label-names)
    (.labelNames metric (into-array (mapv ->prometheus-name label-names)))
    metric))

(defn set-basics [metric metric-name help label-names]
  (-> metric
      (.name (->prometheus-name metric-name))
      (maybe-set-label-names label-names)
      (.help help)))

(defn histogram [registry metric-name help label-names
                        {:keys [start width count] :or {start 0} :as bucket-config}]
  (-> (Histogram/build)
      (set-basics metric-name help label-names)
      (.linearBuckets (double start) (double width) (int count))
      (.register registry)))

(defn counter [registry metric-name help label-names]
  (-> (Counter/build)
      (set-basics metric-name help label-names)
      (.register registry)))

(defn gauge [registry metric-name help label-names]
  (-> (Gauge/build)
      (set-basics metric-name help label-names)
      (.register registry)))

(defn metric [registry {:keys [metric-name metric-type label-names] :as event}]
  (case metric-type
    "counter" (counter registry metric-name "help-text" label-names)
    "gauge" (gauge registry metric-name "help-text" label-names)))
