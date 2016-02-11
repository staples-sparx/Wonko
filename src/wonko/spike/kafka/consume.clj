(ns wonko.spike.kafka.consume
  (:require [cheshire.core :as json]
            [clj-kafka.consumer.zk :as kc]
            [clj-kafka.core :as k])
  (:import [kafka.consumer ConsumerIterator KafkaStream]
           [com.fasterxml.jackson.core JsonParseException]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; A simple kafka consumer client for wonko. Currently does not preserve ;;
;;  the 'offset' across executions. `consume-stream` is currently single ;;
;;  threaded. Use Kafka streams and threads as in https://goo.gl/pfxinq  ;;
;;  to scale this.                                                       ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def config {"zookeeper.connect" "localhost:2182"
             "group.id" "clj-kafka.consumer"
             "auto.offset.reset" "smallest"
             "auto.commit.enable" "false"})

(defn parse [msg]
  (try
    (-> (k/to-clojure msg)
        :value
        (#(String. %))
        (json/decode true))
    (catch JsonParseException e
      (prn "Couldn't parse Message"))))

(defn consume-stream [topic process-fn]
  (k/with-resource [c (kc/consumer config)]
    kc/shutdown
    (let [stream (kc/create-message-stream c topic)
          ^ConsumerIterator it (.iterator ^KafkaStream stream)]
      (while (.hasNext it)
        (process-fn topic (parse (.next it)))))))
