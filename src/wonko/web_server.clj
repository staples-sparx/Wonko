(ns wonko.web-server
  (:require [compojure.core :as cc]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.adapter.jetty :as ring-jetty]
            [wonko.config :as config]
            [wonko.export.prometheus :as prometheus])
  (:import [org.eclipse.jetty.server.handler StatisticsHandler]))

(defonce ^:private web-server (atom nil))

(cc/defroutes wonko-routes
  (cc/GET "/:service/metrics" [service]
          (prometheus/metrics-endpoint service))
  (cc/POST "/:service/clear-data" [service]
           (prometheus/clear-service-data! (config/lookup :prometheus-endpoint) service))
  (route/not-found "Not Found"))

(def wonko-site
  (handler/site wonko-routes))

(defn start []
  (let [port 12000]
    (->> {:port  port :join? false}
         (ring-jetty/run-jetty #'wonko-site)
         (reset! web-server))))

(defn stop []
  (when @web-server
    (.stop @web-server)
    (reset! web-server nil)))
