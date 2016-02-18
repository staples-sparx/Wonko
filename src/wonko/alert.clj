(ns wonko.alert
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [wonko.utils :as utils]
            [kits.logging.log-async :as log]))

(def thread-pool
  (atom nil))

(defn deinit! []
  (.shutdownNow @thread-pool))

(defn init! [thread-pool-size]
  (reset! thread-pool (utils/create-thread-pool thread-pool-size)))

(defn send-alert? [event]
  (get-in event [:options :alert]))

(defn alert-info [topic event]
  {:description (:metric-name event)
   :details (assoc event :topic topic)})

(defn send-alert [{:keys [api-endpoint api-key] :as config} topic event]
  (try
    (let [body (merge {:service_key api-key
                       :event_type "trigger"}
                      (alert-info topic event))]
      (http/post api-endpoint
                 {:content-type :json
                  :body (json/encode body)
                  :throw-exceptions true})
      (log/info {:ns :alert :msg "Sent an alert to pagerduty"})
      true)
    (catch Exception e
      (log/warn {:ns :alert :msg "Could not send alert to pagerduty"
                 :error-message (.getMessage e)
                 :error-trace (map str (.getStackTrace e))})
      false)))

(defn pager-duty [config topic event]
  (when (send-alert? event)
    (.submit @thread-pool #(send-alert config topic event))))
