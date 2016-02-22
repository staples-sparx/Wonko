(ns wonko.core
  (:require [cider.nrepl :as cider]
            [clojure.tools.cli :as cli]
            [clojure.tools.nrepl.server :as nrepl]
            [kits.logging.log-async :as log]
            [refactor-nrepl.middleware :as refactor-nrepl]
            [wonko.alert :as alert]
            [wonko.config :as config]
            [wonko.event-source :as event-source]
            [wonko.export.prometheus :as prometheus]
            [wonko.kafka.consume :as consume]
            [wonko.web-server :as web-server]))

(defn process [event]
  (alert/pager-duty (config/lookup :pager-duty) event)
  (prometheus/register-event event))

(defn start []
  (log/start-thread-pool! (config/lookup :log))
  (alert/init! (config/lookup :alert-thread-pool-size))
  (consume/init! (config/lookup :kafka :consumer))
  (consume/start (config/lookup :kafka :topic-streams) process)
  (web-server/start))

(defn stop [thread-pool]
  (alert/deinit!)
  (consume/stop thread-pool)
  (web-server/stop))

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
