(ns wonko.spike.kafka.produce
  (:require [cheshire.core :as json]
            [clj-kafka.new.producer :as kp])
  (:import [org.apache.kafka.common.serialization Serializer]
           [org.apache.kafka.clients.producer Producer]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; This is the namespace that would be used by services like krikkit/EP ;;
;; to send monitoring events to wonko. This would probably go into kits ;;
;; or some shared library.                                              ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftype Jsonizer []
  Serializer
  (configure [_ _ _ ])
  (serialize [_ topic value]
    (.getBytes (json/generate-string value)))
  (close [_]))

(defonce ^Producer producer (atom nil))
(defonce topic (atom ""))

(defn create-producer [config]
  (kp/producer config
               (kp/string-serializer)
               (Jsonizer.)))

(defn send-message [message]
  (let [record (kp/record @topic message)]
    @(kp/send @producer record)))

(defn counter [metric-name properties & {:as options}]
  (send-message {:metric-type :counter
                 :metric-name metric-name
                 :properties properties
                 :options options}))

(defn gauge [metric-name properties metric-value & {:as options}]
  (send-message {:metric-type :gauge
                 :metric-name metric-name
                 :properties properties
                 :metric-value metric-value
                 :options options}))

(defn init! [config]
  (reset! topic (:topic config))
  (reset! producer (create-producer (:producer config))))
