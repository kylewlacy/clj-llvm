(defproject clj-llvm "0.1.0-SNAPSHOT"
  :description "A Clojure compiler that uses LLVM"
  :url "https://github.com/kylewlacy/clj-llvm"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.macro "0.1.2"]
                 [org.clojure/tools.analyzer "0.3.0"]
                 [mjolnir "0.1.5"]]
  :main clj-llvm.compiler)
