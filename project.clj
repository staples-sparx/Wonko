(defproject wonko "0.1.0-SNAPSHOT"
  :local-repo ".m2"
  :description "Host node and Application monitoring service"
  :url "git@github.com:staples-sparx/Wonko.git"
  :repositories {"runa-maven-s3" {:url "s3p://runa-maven/releases/"
                                  :username [:gpg :env/archiva_username]
                                  :passphrase [:gpg :env/archiva_passphrase]}}
  :dependencies [[cheshire "5.5.0"]
                 [cider/cider-nrepl "0.11.0-SNAPSHOT"]
                 [clj-http "2.0.0"]
                 [clj-kafka "0.3.4"]
                 [compojure "1.4.0"]
                 [io.prometheus/simpleclient "0.0.11"]
                 [io.prometheus/simpleclient_common "0.0.11"]
                 [io.prometheus/simpleclient_hotspot "0.0.11"]
                 [io.prometheus/simpleclient_servlet "0.0.11"]
                 [org.clojars.runa/kits "1.20.1"
                  :exclusions [org.eclipse.jetty/jetty-server]]
                 [org.clojure/clojure "1.7.0"]
                 [org.clojure/tools.cli "0.3.3"]
                 [org.clojure/tools.nrepl "0.2.12"]
                 [refactor-nrepl "2.2.0-SNAPSHOT"]
                 [ring/ring-core "1.4.0"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [staples-sparx/wonko-client "0.1.4"]
                 [gui-diff "0.6.7"]
                 [org.clojure/java.jdbc "0.6.0-rc1"]
                 [postgresql/postgresql "9.3-1101.jdbc4"]]
  :main ^:skip-aot wonko.core
  :plugins [[s3-wagon-private "1.2.0"]]
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
