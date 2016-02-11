(ns wonko.spike.export
  (:require [wonko.spike.export.prometheus :as prometheus]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Convert metrics to send to riemann or prometheus or other monitoring ;;
;; solution. This currently creates prometheus metrics dynamically.     ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; This contains prometheus created metrics in a map of the form:
;; {:topic {:metric-type {metric-name metric}}}
(defonce created-metrics
  (atom {}))

(defn create-metric [{:keys [metric-name metric-type options] :as event}]
  (case metric-type
    "counter" (prometheus/create-counter metric-name "help-text" [])
    "gauge" (prometheus/create-gauge metric-name "help-text" [])))

(defn get-or-create-metric [topic {:keys [metric-name metric-type options] :as event}]
  (let [metric-path [topic (keyword metric-type) metric-name]]
    (or (get-in @created-metrics metric-path)
        (let [created-metric (create-metric event)]
          (swap! created-metrics assoc-in metric-path created-metric)
          created-metric))))

(defn prometheus [topic {:keys [metric-value] :as event}]
  (try
    (prometheus/register (get-or-create-metric topic event) [] metric-value)
    (catch IllegalArgumentException e
      nil)))
