(ns wonko.spike.kafka.produce
  (:require [cheshire.core :as json]
            [clj-kafka.new.producer :as kp])
  (:import [org.apache.kafka.common.serialization Serializer]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; This is the namespace that would be used by services like krikkit/EP ;;
;; to send monitoring events to wonko. This would probably go into kits ;;
;; or some shared library.                                              ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def config
  {"bootstrap.servers" "127.0.0.1:9092"
   "compression.type" "gzip"
   "linger.ms" 5})

(deftype Jsonizer []
  Serializer
  (configure [_ _ _ ])
  (serialize [_ topic value]
    (.getBytes (json/generate-string value)))
  (close [_]))

(defn create-producer []
  (kp/producer config
               (kp/string-serializer)
               (Jsonizer.)))

(defn send-message [producer topic message]
  (let [record (kp/record topic message)]
    @(kp/send producer record)))

(defn counter [producer topic metric-name & {:as options}]
  (send-message producer
                topic
                {:metric-type :counter
                 :metric-name metric-name
                 :options options}))

(defn gauge [producer topic metric-name metric-value & {:as options}]
  (send-message producer
                topic
                {:metric-type :gauge
                 :metric-name metric-name
                 :metric-value metric-value
                 :options options}))
