(ns wonko.kafka.admin
  (require [clj-kafka.admin :as admin]
           [clj-kafka.zk :as zk]))

(defn create-topic [topic]
  (with-open [zk (admin/zk-client "127.0.0.1:2182")]
    (if-not (admin/topic-exists? zk topic)
      (admin/create-topic zk topic
                          {:partitions 1
                           :replication-factor 1
                           :config {"cleanup.policy" "compact"}}))))

(defn delete-topic [topic]
  (with-open [zk (admin/zk-client "127.0.0.1:2182")]
    (admin/delete-topic zk topic)))

(defn list-topics []
  (zk/topics {"zookeeper.connect" "127.0.0.1:2182"}))

(defn delete-all-topics []
  (for [topic (list-topics)]
    (delete-topic topic)))
