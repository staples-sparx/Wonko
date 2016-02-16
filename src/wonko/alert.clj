(ns wonko.alert
  (:require [clj-http.client :as http]
            [cheshire.core :as json]))

(defn send-alert? [event]
  (get-in event [:options :alert]))

(defn alert-info [topic event]
  {:description (:metric-name event)
   :details (assoc event :topic topic)})

(defn pager-duty [{:keys [api-endpoint api-key] :as config} topic event]
  (when (send-alert? event)
    (try
      (let [body (merge {:service_key api-key
                         :event_type "trigger"}
                        (alert-info topic event))]
        (http/post api-endpoint
                   {:content-type :json
                    :body (json/encode body)
                    :throw-exceptions true})
        (spit "wonko.log" "Sent an alert to pagerduty!\n" :append true))
      (catch Exception e
        (spit "wonko.log" "Posting to pagerduty failed!\n" :append true)))))
