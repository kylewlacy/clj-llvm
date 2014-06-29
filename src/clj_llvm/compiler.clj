(ns clj-llvm.compiler
  (:require [clojure.pprint    :refer [pprint]]
            [clojure.java.io   :refer [reader]]
            [clj-llvm.analyzer :as    analyzer]))

(defn -main [filename & args]
  (pprint (analyzer/analyze-file filename (analyzer/empty-env))))
