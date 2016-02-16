(ns wonko.core
  (:require [clojure.tools.cli :as cli]
            [cider.nrepl :as cider]
            [clojure.tools.nrepl.server :as nrepl]
            [refactor-nrepl.middleware :as refactor-nrepl]
            [wonko.config :as config]
            [wonko.kafka.admin :as admin]
            [wonko.alert :as alert]
            [wonko.export.prometheus :as prometheus]
            [wonko.kafka.consume :as consume]
            [wonko.event-source :as event-source]))

(defn process [topic event]
  (alert/pager-duty (config/pager-duty) topic event)
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
