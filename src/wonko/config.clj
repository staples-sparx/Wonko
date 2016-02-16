(ns wonko.config)

(defn consumer []
  {"zookeeper.connect" "localhost:2182"
   "group.id" "clj-kafka.consumer"
   "auto.offset.reset" "smallest"
   "auto.commit.enable" "false"})

(defn topic-streams []
  {"krikkit" 2})


(defn pager-duty []
  {:api-endpoint "https://events.pagerduty.com/generic/2010-04-15/create_event.json"
   :api-key "4bbd297e0f7e43cc948f7894b7d8ec7b"})
