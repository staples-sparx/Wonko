(defproject wonko "0.1.0-SNAPSHOT"
  :description "SparX Platform and Services monitoring application"
  :url "git@github.com:staples-sparx/Wonko.git"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [clj-kafka "0.3.4"]
                 [cheshire "5.5.0"]
                 [io.prometheus/simpleclient "0.0.11"]
                 [io.prometheus/simpleclient_hotspot "0.0.11"]
                 [io.prometheus/simpleclient_servlet "0.0.11"]
                 [io.prometheus/simpleclient_common "0.0.11"]]
  :main ^:skip-aot wonko.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
