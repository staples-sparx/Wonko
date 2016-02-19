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

(defn consumer []
  (lookup :consumer))

(defn topic-streams []
  (lookup :topic-streams))

(defn pager-duty []
  (lookup :pager-duty))

(defn alert-thread-pool-size []
  (lookup :alert-thread-pool-size))

(defn log []
  (lookup :log))
