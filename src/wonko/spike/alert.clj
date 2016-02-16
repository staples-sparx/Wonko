(ns wonko.spike.alert
  (:require [clj-http.client :as http]
            [cheshire.core :as json]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; This namespace will configure, and if necessary, send alerts via    ;;
;; email, pager-duty, etc. Each of which can be under this. See        ;;
;; kits.alerts and eccentrica.utils.alerts namespaces for integrations ;;
;; with pager duty.                                                    ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn pager-duty [{:keys [api-endpoint api-key] :as config}
                  {:keys [metric-type metric-name properties] :as event}]
  (try
    (let [body {:service_key api-key
                :event_type "trigger"
                :description metric-name
                :details event}]
      (http/post api-endpoint
                 {:content-type :json
                  :body (json/encode body)
                  :throw-exceptions true})
      (spit "wonko.log" "Sent an alert to pagerduty!\n" :append true))
    (catch Exception e
      (spit "wonko.log" "Posting to pagerduty failed!\n" :append true))))
