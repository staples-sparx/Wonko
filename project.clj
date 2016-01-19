(defproject wonko "0.1.0-SNAPSHOT"
  :description "SparX Platform and Services monitoring application"
  :url "git@github.com:staples-sparx/Wonko.git"
  :dependencies [[org.clojure/clojure "1.7.0"]]
  :main ^:skip-aot wonko.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
