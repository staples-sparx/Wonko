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

(defonce stop?
  (atom false))

(defn stop []
  (swap! stop? (constantly true)))


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

(defn consume-a-stream [topic stream process-fn]
  (future ;; put this into a thread from a threadpool
    (spit "wonko.log" (str {:msg "starting to consume a stream" :topic topic} "\n") :append true)
    (let [^ConsumerIterator it (.iterator ^KafkaStream stream)]
      (spit "wonko.log" (str  "Has next?" (.hasNext it) "\n") :append true)
      (while (and (.hasNext it) (not @stop?))
        (let [event (parse (.next it))]
          (process-fn topic event)
          (spit "wonko.log" (str event "\n") :append true))))))

(defn consume-topics [topic-stream-config process-fn]
  (swap! stop? (constantly false))
  (k/with-resource [c (kc/consumer config)]
    kc/shutdown
    (let [all-topic-streams (kc/create-message-streams c topic-stream-config)
          jobs (for [[topic streams] all-topic-streams
                     stream streams]
                 (consume-a-stream topic stream process-fn))]
      (doall (map deref jobs)))))

(comment
  (consume-topics {"krikkit" 2}))
