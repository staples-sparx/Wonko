(ns wonko.test-config)

(def zookeeper-config {"zookeeper.connect" "localhost:2182"
                       "group.id" "clj-kafka.consumer"
                       "auto.offset.reset" "smallest"
                       "auto.commit.enable" "true"
                       "auto.commit.interval.ms" "1000"})

(def kafka-config {"bootstrap.servers" "127.0.0.1:9092"
                   "compression.type" "gzip"
                   "linger.ms" 5})
