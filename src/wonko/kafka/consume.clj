(ns wonko.kafka.consume
  (:require [cheshire.core :as json]
            [clj-kafka.consumer.zk :as kc]
            [clj-kafka.core :as k])
  (:import [kafka.consumer ConsumerIterator Consumer KafkaStream]
           [com.fasterxml.jackson.core JsonParseException]))

(defonce ^Consumer consumer
  (atom nil))

(defn init! [config]
  (reset! consumer (kc/consumer config)))

(defn parse [msg]
  (try
    (-> (k/to-clojure msg)
        :value
        (#(String. %))
        (json/decode true))
    (catch JsonParseException e
      (spit "wonko.log" (str "Couldn't parse Message" "\n") :append true))))

(defn consume-a-stream [topic stream process-fn]
  (future ;; put this into a thread from a threadpool
    (spit "wonko.log" (str {:msg "starting to consume a stream" :topic topic} "\n") :append true)
    (let [^ConsumerIterator it (.iterator ^KafkaStream stream)]
      (spit "wonko.log" (str  "Has next?" (.hasNext it) "\n") :append true)
      (loop []
        (when (.hasNext it)
          (let [event (parse (.next it))]
            (spit "wonko.log" (str "processing " event "\n") :append true)
            (process-fn topic event)))
        (recur)))))

(defn start-consuming-topics [topic-stream-config process-fn]
  (let [all-topic-streams (kc/create-message-streams @consumer topic-stream-config)
        jobs (doall (for [[topic streams] all-topic-streams
                          stream streams]
                      (consume-a-stream topic stream process-fn)))]
    jobs))

(defn stop-consuming-topics [jobs]
  (doseq [job jobs]
    (future-cancel job))
  (kc/shutdown @consumer))

(comment
  (consume-topics {"krikkit" 2}))
