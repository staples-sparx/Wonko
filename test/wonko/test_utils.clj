(ns wonko.test-utils
  (:require [wonko.kafka.admin :as admin]
            [wonko-client.core :as client]))

(def test-kafka-config
  {"bootstrap.servers" "127.0.0.1:9092"
   "compression.type" "gzip"
   "linger.ms" 5})

(defn wait-for
  "Invoke predicate every interval (default 10) seconds until it returns true,
  or timeout (default 150) seconds have elapsed. E.g.:

  (wait-for #(< (rand) 0.2) :interval 1 :timeout 10)

  Returns nil if the timeout elapses before the predicate becomes true, otherwise
  the value of the predicate on its last evaluation."
  [predicate & {:keys [interval timeout sleep]
                :or {interval 1
                     timeout 5
                     sleep 1}}]
  (Thread/sleep (* sleep 1000))
  (let [end-time (+ (System/currentTimeMillis) (* timeout 1000))]
    (loop []
      (if-let [result (predicate)]
        result
        (do
          (Thread/sleep (* interval 1000))
          (if (< (System/currentTimeMillis) end-time)
            (recur)))))))

(defn rand-str [prefix]
  (->> (.nextInt (java.util.concurrent.ThreadLocalRandom/current) 0 999999)
       (str prefix "-")))

(defn prometheus-registry->map [registry]
  (->> registry
       .metricFamilySamples
       enumeration-seq
       (map #(.-samples %))
       (map first)
       (map (fn [i] [(.name i) (.value i)]))
       (into {})))

(defn metric-samples [histogram]
  (for [sample (.-samples (first (.collect histogram)))]
    {:label-names (.labelNames sample)
     :label-values (.labelValues sample)
     :name (.name sample)
     :value (.value sample)}))

(defn find-metric-sample [histogram metric-name]
  (->> histogram
       metric-samples
       (filter #(= metric-name (:name %)))
       first))

(defn init-client [service-name events-topic alerts-topic]
  (client/init! service-name test-kafka-config :validate? true)
  (client/set-topics! events-topic alerts-topic))

(defn create-topics []
  (let [events-topic (rand-str "test-events")
        alerts-topic (rand-str "test-alerts")]
    (admin/create-topic events-topic)
    (admin/create-topic alerts-topic)
    {:events-topic events-topic
     :alerts-topic alerts-topic}))

(defn delete-topics [events-topic alerts-topic]
  (admin/delete-topic events-topic)
  (admin/delete-topic alerts-topic))
