(ns wonko.test-utils)

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

(defn rand-topic-name []
  (->> (.nextInt (java.util.concurrent.ThreadLocalRandom/current) 0 999999)
       (str "test-topic-")))


(defn prometheus-registry->map [registry]
  (->> registry
       .metricFamilySamples
       enumeration-seq
       (map #(.-samples %))
       (map first)
       (map (fn [i] [(.name i) (.value i)]))
       (into {})))
