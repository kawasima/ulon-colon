(defproject net.unit8/ulon-colon.browser "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [net.unit8/ulon-colon "0.1.0-SNAPSHOT"]
                 [net.unit8/clj-flume-node "0.1.0"]
                 [hiccup "1.0.5"]
                 [compojure "1.1.6"]
                 [ring/ring-devel "1.2.1"]
                 [ring/ring-jetty-adapter "1.2.1"]
                 [incanter "1.5.4"]
                 [org.clojure/clojurescript "0.0-2156" :scope "provided"]
                 [jayq "2.5.0"]]
  :plugins [[lein-cljsbuild "1.0.2"]
            [lein-ring "0.8.10"]]
  :source-paths ["src/clj"]
  :cljsbuild
  { :builds [{:source-paths ["src/cljs"]
              :compiler {:optimizations :simple
                         :output-to "resources/public/js/example.js"}
              :incremental false}
              ]}
;  :aot :all
  :main ulon-colon.browser.core
  :ring {:handler ulon-colon.browser.core/app})
