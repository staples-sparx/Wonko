(ns wonko.kafka.consume
  (:require [cheshire.core :as json]
            [clj-kafka.consumer.zk :as kc]
            [clj-kafka.core :as k]
            [wonko.utils :as utils]
            [kits.logging.log-async :as log])
  (:import [kafka.consumer ConsumerIterator Consumer KafkaStream]
           [com.fasterxml.jackson.core JsonParseException]))

(defonce ^Consumer consumer
  (atom nil))

(defn init! [config]
  (reset! consumer (kc/consumer config)))

(defn parse [msg]
  (let [string-msg (-> (k/to-clojure msg) :value (#(String. %)))]
    (try
      (json/decode string-msg true)
      (catch JsonParseException e
        (log/warn {:ns :consume :msg "Couldn't parse message"
                   :kafka-msg string-msg
                   :error-message (.getMessage e)
                   :error-trace (map str (.getStackTrace e))})))))

(defn consume-a-stream [topic stream process-fn]
  (log/info  {:ns :consume :msg "starting to consume a stream" :topic topic})
  (let [^ConsumerIterator it (.iterator ^KafkaStream stream)]
    (log/debug {:ns :consume :msg (str "Does the stream have more items? " (.hasNext it))})
    (loop []
      (when (.hasNext it)
        (let [event (parse (.next it))]
          (log/debug {:ns :consume :msg (str "processing " event)})
          (try (process-fn topic event)
               (catch Exception e
                 (log/warn {:ns :consume :msg "Unable to process an event from kafka"
                            :kafka-event event
                            :error-message (.getMessage e)
                            :error-trace (map str (.getStackTrace e))})))))
      (recur))))

(defn start-consuming-topics [topic-stream-config process-fn]
  (let [thread-pool (utils/create-thread-pool (apply + (vals topic-stream-config)))
        all-topic-streams (kc/create-message-streams @consumer topic-stream-config)
        jobs (doall (for [[topic streams] all-topic-streams
                          stream streams]
                      (.submit thread-pool #(consume-a-stream topic stream process-fn))))]
    thread-pool))

(defn stop-consuming-topics [thread-pool]
  (kc/shutdown @consumer)
  (.shutdownNow thread-pool))

(comment
  (consume-topics {"krikkit" 2}))
