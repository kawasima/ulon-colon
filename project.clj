(defproject net.unit8/ulon-colon "0.2.3"
  :description "Message transfer system without a queue."
  :url "https://github.com/kawasima/ulon-colon"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/data.fressian "0.2.1"]
                 [org.clojure/clojurescript "1.7.122" :scope "provided"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [io.netty/netty-codec-http "4.0.23.Final"]
                 [net.unit8/fressian-cljs "0.2.0"]
                 [junit "4.12" :scope "test"]]

  :plugins [[lein-cljsbuild "1.1.0"]]
  :source-paths ["src/clj" "src/cljs"]
  :java-source-paths ["src/java"]
  :test-paths ["test/clj" "test/java"]
  :cljsbuild
  {:builds
   {:dev
    {:source-paths ["src/cljs"]
     :jar false
     :compiler {:output-to "resources/public/js/main-debug.js"
                :optimizations :whitespace
                :pretty-print true}}}})
