(ns cljl.core
  (:require [cljl.compiler :as compiler]))

(defn -main [input-file main-ns output-exe & args]
  (compiler/compile-file input-file main-ns output-exe)
  (println "Done!")
  (System/exit 0))
