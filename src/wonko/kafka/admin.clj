(ns wonko.kafka.admin
  (require [clj-kafka.admin :as admin]
           [clj-kafka.zk :as zk]
           [wonko.config :as config]))

(defn create-topic [topic]
  (with-open [zk (admin/zk-client (config/zookeeper))]
    (if-not (admin/topic-exists? zk topic)
      (admin/create-topic zk
                          topic
                          (config/lookup :kafka :new-topic)))))

(defn delete-topic [topic]
  (with-open [zk (admin/zk-client (config/zookeeper))]
    (admin/delete-topic zk topic)))

(defn list-topics []
  (zk/topics {"zookeeper.connect" (config/zookeeper)}))

(defn delete-all-topics []
  (for [topic (list-topics)]
    (delete-topic topic)))
