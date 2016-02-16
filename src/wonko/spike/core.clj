(ns wonko.spike.core
  (:require [clojure.tools.cli :as cli]
            [cider.nrepl :as cider]
            [clojure.tools.nrepl.server :as nrepl]
            [refactor-nrepl.middleware :as refactor-nrepl]
            [wonko.spike.config :as config]
            [wonko.spike.kafka.admin :as admin]
            [wonko.spike.alert :as alert]
            [wonko.spike.export.prometheus :as prometheus]
            [wonko.spike.kafka.consume :as consume]
            [wonko.spike.event-source :as event-source]))

(defn process [topic event]
  (alert/pager-duty (config/pager-duty) event)
  (prometheus/register-event topic event))

(defn start []
  (consume/init! (config/consumer))
  (consume/start-consuming-topics (config/topic-streams) process))

(defn stop [jobs]
  (consume/stop-consuming-topics jobs))

(defn- start-nrepl! [port]
  (nrepl/start-server
   :port port
   :handler (-> cider/cider-nrepl-handler refactor-nrepl/wrap-refactor))
  (println "Started nREPL server on port" port))

(defn -main [& args]
  (let [{:keys [options arguments errors summary]}
        (cli/parse-opts args [["-n" "--name NAME" "Service Name" :default "wonko"]
                              ["-p"    "--port" "HTTP Port"
                               :default 12000 :parse-fn #(Long/parseLong %)]
                              ["-np"   "--nrepl-port" "nREPL port"
                               :default 12001 :parse-fn #(Long/parseLong %)]])]
    (start-nrepl! (:nrepl-port options))
    (start)))

(comment
  (event-source/krikkit)
  (event-source/eccentrica)
  (def fs (start))
  (stop fs))
