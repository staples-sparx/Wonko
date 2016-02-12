(ns wonko.spike.export
  (:require [wonko.spike.export.prometheus :as prometheus]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Convert metrics to send to riemann or prometheus or other monitoring ;;
;; solution. This currently creates prometheus metrics dynamically.     ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; This contains prometheus created metrics in a map of the form:
;; {:topic {:metric-type {metric-name metric} :registry registry}}
(defonce created-metrics
  (atom {}))

(defn create-metric [registry {:keys [metric-name metric-type options] :as event}]
  (case metric-type
    "counter" (prometheus/create-counter registry metric-name "help-text" [])
    "gauge" (prometheus/create-gauge registry metric-name "help-text" [])))

(defn get-or-create-metric [registry topic {:keys [metric-name metric-type options] :as event}]
  (let [metric-path [topic (keyword metric-type) metric-name]]
    (or (get-in @created-metrics metric-path)
        (let [created-metric (create-metric registry event)]
          (swap! created-metrics assoc-in metric-path created-metric)
          created-metric))))

(defn get-or-create-registry [topic]
  (let [registry-path [topic :registry]]
    (or (get-in @created-metrics registry-path)
        (let [created-registry (prometheus/create-registry)]
          (swap! created-metrics assoc-in registry-path created-registry)
          created-registry))))

(defn prometheus [topic {:keys [metric-value] :as event}]
  (try
    (let [registry (get-or-create-registry topic)]
      (prometheus/register (get-or-create-metric registry topic event)
                           registry
                           []
                           metric-value))
    (catch IllegalArgumentException e
      nil)))

(defn metrics-endpoint [topic]
  (prometheus/metrics-endpoint (get-or-create-registry topic)))
