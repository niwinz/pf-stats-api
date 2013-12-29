(defproject pfrt "0.1.0"
  :description "Pr0n-Stars"
  :url "http://example.com/FIXME"
  :license {:name "Apache 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.txt"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [http-kit "2.1.12"]
                 [compojure "1.1.6"]
                 [org.clojure/data.json "0.2.3"]
                 [ring/ring-jetty-adapter "1.2.1"]
                 ;; [hiccup "1.0.4"]
                 ;; [be.niwi/clj.jdbc "0.1.0-beta4"]
                 [org.clojure/algo.monads "0.1.4"]
                 [jarohen/nomad "0.6.0"]
                 [swiss-arrows "1.0.0"]

                 ;; ClojureScript Dependencies
                 [org.clojure/clojurescript "0.0-2127"]
                 [im.chit/purnam "0.1.8"]
                 [crate "0.2.5"]
                 [jayq "2.5.0"]]
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
