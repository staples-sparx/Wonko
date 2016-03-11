(ns wonko.config
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(defonce ^:private config-resource
  (-> "config.edn" io/resource))

(defn read-config
  ([] (read-config config-resource))
  ([config] (-> config slurp edn/read-string)))

(defonce current (atom (read-config)))

(defn get-current []
  @current)

(defn lookup [& ks]
  (get-in @current ks))

(defn reload []
  (reset! current (read-config)))

(defn zookeeper []
  (lookup :kafka :consumer "zookeeper.connect"))

(defn production-env? []
  (= "production" (lookup :env)))
