(ns wonko.spike.web-server
  (:require [wonko.spike.config :as config]
            [compojure.core :as cc]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [ring.adapter.jetty :as ring-jetty]
            [wonko.spike.export.prometheus :as prometheus])
  (:import [org.eclipse.jetty.server.handler StatisticsHandler]))

(defonce ^:private web-server (atom nil))

(cc/defroutes wonko-routes
  (cc/GET "/krikkit/metrics" []
          (prometheus/metrics-endpoint "krikkit"))
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
