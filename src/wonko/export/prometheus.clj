(ns wonko.export.prometheus
  (:require [clojure.string :as s]
            [clj-http.client :as http]
            [kits.logging.log-async :as log]
            [ring.util.response :as res]
            [wonko.config :as config]
            [wonko.export.prometheus.create :as create]
            [wonko.export.prometheus.register :as register]
            [wonko.test-utils :as tu])
  (:import io.prometheus.client.exporter.common.TextFormat
           io.prometheus.client.hotspot.DefaultExports
           java.util.concurrent.locks.ReentrantLock
           java.io.StringWriter))

(defonce
  ^{:doc "This contains prometheus created metrics in a map of the form:
          {:service {:metric-type {metric-name metric} :registry registry}}"}
  created-metrics
  (atom {}))

(defonce
  ^{:doc "Used to regulate access to registry creation.
          TODO: At some point we might need finer-grained locking,
                but this seems adequate for now."}
  lock (Object.))

(defn get-label-names [properties]
  (->> [:host :ip-address]
       (concat (keys properties))
       sort))

(defn get-label-values [metadata properties]
  (map (merge properties metadata)
       (get-label-names properties)))

(defn get-or-create-metric [registry {:keys [service metric-name metric-type properties] :as event}]
  (locking registry
    (let [metric-path [service (keyword metric-type) metric-name]
          label-names (get-label-names properties)]
      (or (get-in @created-metrics metric-path)
          (let [created-metric (create/metric registry (assoc event :label-names label-names))]
            (swap! created-metrics assoc-in metric-path created-metric)
            created-metric)))))

(defn get-or-create-registry [service]
  (locking lock
    (let [registry-path [service :registry]]
      (or (get-in @created-metrics registry-path)
          (let [created-registry (create/registry)]
            (swap! created-metrics assoc-in registry-path created-registry)
            created-registry)))))

(defn register-event [{:keys [service metric-value metadata properties] :as event}]
  (try
    (let [registry (get-or-create-registry service)
          metric (get-or-create-metric registry event)
          label-values (get-label-values metadata properties)]
      (register/metric metric (assoc event :label-values label-values)))
    (catch Exception e
      (log/info {:msg "unable to register event in prometheus"
                 :event event
                 :exception (bean e)})
      (throw e))))

(defn metrics-endpoint [service]
  (let [registry (get-in @created-metrics [service :registry])
        writer (StringWriter.)]
    (if registry
      (do (TextFormat/write004 writer (.metricFamilySamples registry))
          {:status  200
           :headers {"Content-Type" TextFormat/CONTENT_TYPE_004}
           :body    (.toString writer)})
      (res/not-found "Not found"))))

(defn clear-service-data!
  "Warning! This will remove all export related data for the given service.
  Use this only in development or test environments."
  [endpoint service]
  (if (config/production-env?)
    {:status 403
     :body "Clearing service data is not allowed in production."}
    (do
      (swap! created-metrics assoc service {})
      (http/delete (format "http://%s/api/v1/series" endpoint)
                   {:query-params {"match[]" (format "{job=\"%s\"}" service)}}))))
