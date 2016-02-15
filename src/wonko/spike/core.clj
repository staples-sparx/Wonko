(ns wonko.spike.core
  (:require [wonko.spike.admin :as admin]
            [wonko.spike.alert :as alert]
            [wonko.spike.export :as export]
            [wonko.spike.kafka.consume :as consume]
            [wonko.spike.event-source :as event-source]))

(defn process [topic event]
  (alert/pager-duty event)
  (export/prometheus topic event)
  (prn event))

(defn start []
  (consume/consume-topics {"krikkit" 2} process))

(comment
  (event-source/krikkit)
  (event-source/eccentrica)
  (start))
