(defproject cljl "0.1.0-SNAPSHOT"
  :description "A Clojure compiler that uses LLVM"
  :url "https://github.com/kylewlacy/cljl"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.macro "0.1.2"]
                 [org.clojure/tools.analyzer "0.3.0"]
                 [net.java.dev.jna/jna "3.4.0"]]
  :profiles {:dev {:dependencies [[speclj "3.0.0"]]}}
  :plugins [[speclj "3.0.0"]]
  :test-paths ["spec"]
  :main cljl.core)
