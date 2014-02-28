(defproject pfrt "0.1.0"
  :description "Pr0n-Stars"
  :url "http://example.com/FIXME"
  :license {:name "Apache 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.txt"}
  :dependencies [[org.clojure/clojure "1.6.0-beta1"]
                 [http-kit "2.1.16"]
                 [compojure "1.1.6"]
                 [org.clojure/data.json "0.2.3"]
                 [org.clojure/tools.namespace "0.2.4"]

                 ;; Jetty9 + Explict Servlet Api Version
                 [info.sunng/ring-jetty9-adapter "0.5.0"]
                 [javax.servlet/javax.servlet-api "3.1.0"]

                 ;; Other utils
                 [org.clojure/algo.monads "0.1.4"]
                 [jarohen/nomad "0.6.0"]
                 [swiss-arrows "1.0.0"]

                 ;; ClojureScript Dependencies
                 [org.clojure/clojurescript "0.0-2127"]
                 [domina "1.0.3-SNAPSHOT"]
                 [crate "0.2.5"]]
  :main ^:skip-aot pfrt.main
  :target-path "target/%s"
  :source-paths ["src/clj" "src/cljs"]
  :plugins [[lein-cljsbuild "1.0.1"]]
  :profiles {:uberjar {:aot :all
                       :hooks [leiningen.cljsbuild]}}
  :cljsbuild {
    :builds [{
        :source-paths ["src/cljs"]
        :compiler {
          :output-to "resources/public/js/main.js"  ; default: target/cljsbuild-main.js
          :optimizations :whitespace
          :externs ["resources/public/js/jquery.js"]
          :pretty-print true}}]})
