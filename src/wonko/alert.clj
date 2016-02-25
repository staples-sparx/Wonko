(ns wonko.alert
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [wonko.utils :as utils]
            [kits.logging.log-async :as log]))

(defonce thread-pool
  (atom nil))

(defn deinit! []
  (.shutdownNow @thread-pool))

(defn init! [thread-pool-size]
  (reset! thread-pool (utils/create-thread-pool thread-pool-size)))

(defn send-alert? [event]
  (boolean (get-in event [:alert-name])))

(defn alert-info [event]
  {:description (:alert-name event)
   :details (:alert-info event)})

(defn send-alert [api-endpoint api-key event]
  (try
    (let [body (merge {:service_key api-key
                       :event_type "trigger"}
                      (alert-info event))]
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

(defn pager-duty [config event]
  (when (send-alert? event)
    (let [api-endpoint (:api-endpoint config)
          api-key (get-in config [:api-keys (:service event)])]
      (if api-key
        (.submit @thread-pool #(send-alert api-endpoint api-key event))
        (log/info {:ns :alert :msg "Pagerduty not configured for service" :service (:service event)})))))
