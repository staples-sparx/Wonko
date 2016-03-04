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
  (let [offset (-> (k/to-clojure msg) :offset)
        message (-> (k/to-clojure msg) :value (#(String. %)))]
    (try
      {:message (json/decode message true) :offset offset}
      (catch JsonParseException e
        (log/warn {:ns :consume :msg "Couldn't parse message"
                   :kafka-msg message
                   :kafka-offset offset
                   :error-message (.getMessage e)
                   :error-trace (map str (.getStackTrace e))})))))

(defn consume-a-stream [stream process-fn]
  (log/info  {:ns :consume :msg "starting to consume a stream"})
  (let [^ConsumerIterator it (.iterator ^KafkaStream stream)]
    (log/debug {:ns :consume :msg (str "Does the stream have more items? " (.hasNext it))})
    (loop []
      (when (.hasNext it)
        (let [event (parse (.next it))]
          (log/debug {:ns :consume :msg (str "processing " event)})
          (try (process-fn (:message event))
               (catch Exception e
                 (log/warn {:ns :consume :msg "Unable to process an event from kafka"
                            :kafka-event event
                            :error-message (.getMessage e)
                            :error-trace (map str (.getStackTrace e))})))))
      (recur))))

(defn start [topic-stream-config process-fn]
  (let [thread-pool (utils/create-thread-pool (apply + (vals topic-stream-config)))
        topic->streams (kc/create-message-streams @consumer topic-stream-config)
        jobs (doall (for [[topic streams] topic->streams
                          stream streams]
                      (.submit thread-pool #(consume-a-stream stream process-fn))))]
    thread-pool))

(defn stop [thread-pool]
  (kc/shutdown @consumer)
  (.shutdownNow thread-pool))

(comment
  (consume-topics {"krikkit" 2}))
