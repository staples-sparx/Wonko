(ns wonko.spike.config)

(defn consumer []
  {"zookeeper.connect" "localhost:2182"
   "group.id" "clj-kafka.consumer"
   "auto.offset.reset" "smallest"
   "auto.commit.enable" "false"})

(defn topic-streams []
  {"krikkit" 2})
