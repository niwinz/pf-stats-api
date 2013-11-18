(defproject subprocess "0.1.0"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [http-kit "2.1.12"]
                 [compojure "1.1.6"]
                 [hiccup "1.0.4"]
                 [org.clojure/data.json "0.2.3"]
                 [ring/ring-jetty-adapter "1.2.1"]
                 [com.github.mfornos/humanize-slim "1.1.3"]]
  :main ^:skip-aot subprocess.http
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
