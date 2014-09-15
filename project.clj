(defproject net.unit8/ulon-colon "0.1.0"
  :description "Message transfer system without a queue."
  :url "https://github.com/kawasima/ulon-colon"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.3.0"]
                 [aleph "0.3.3"]
                 [org.clojure/data.fressian "0.2.0"]
                 [org.clojure/clojurescript "0.0-2280" :scope "provided"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [net.unit8/fressian-cljs "0.1.0"]]
  :plugins [[lein-cljsbuild "1.0.2"]]
  :source-paths ["src/clj" "src/cljs"]
  :cljsbuild
  {:builds
   {:dev
    {:source-paths ["src/cljs"]
     :jar false
     :compiler {:output-to "resources/public/js/main-debug.js"
                :optimizations :whitespace
                :pretty-print true}}}})
