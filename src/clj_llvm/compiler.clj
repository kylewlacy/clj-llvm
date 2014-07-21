(ns clj-llvm.compiler
  (:require [clojure.pprint               :refer [pprint]]
            [clojure.java.io              :refer [reader]]
            [clj-llvm.analyzer            :as    analyzer]
            [clj-llvm.llvm.module-builder :as    module-builder]
            [clj-llvm.builder             :as    builder]))

(defn maybe-dump [module options]
  (if (options :dump)
    (module-builder/dump module)
    module))

(defn verify [module options]
  (if (options :verbose)
    (println "Verifying module..."))
  (module-builder/verify module))

(defn maybe-optimize [module options]
  (if (options :optimize)
    (do
      (if (options :verbose)
        (println "Optimizing..."))
        (module-builder/optimize module))
    module))

(defn module-to-assembly [module output-file options]
  (when (options :verbose)
    (println "Writing assembly...")
    (println (str "  " output-file)))
  (module-builder/module-to-assembly module output-file))

(defn assembly-to-executable [assembly-file exe-file options]
  (when (options :verbose)
    (println "Building executable...")
    (println (str "  " exe-file)))
  (module-builder/assembly-to-executable assembly-file exe-file))





(defn get-temp-filename []
  (let [temp-file
          (java.io.File/createTempFile (str (gensym "temp"))
                                       nil
                                       nil)]
    (.deleteOnExit temp-file)
    (.toString temp-file)))

(defn compile-module-to-object
  ([module options]
    (-> module
        (maybe-dump options)
        (verify options)
        (maybe-optimize options)
        (module-builder/module-to-object (get-temp-filename)))))

(defn compile-modules-to-exe
  ([modules output-exe]
    (compile-modules-to-exe modules output-exe {:dump     false
                                                :optimize false
                                                :verbose  false}))
  ([modules output-exe options]
    (module-builder/objects-to-executable
      (map compile-module-to-object modules (repeat options))
      output-exe)))

(defn compile-forms
  ([forms main-ns output-exe]
    (compile-forms forms nil main-ns output-exe))
  ([forms libs main-ns output-exe]
    (compile-modules-to-exe
      (conj (map builder/build-lib-to-module libs (repeat libs))
            (builder/build-asts-to-module main-ns
                                          (analyzer/analyze-forms forms)
                                          libs)
            (builder/build-entry-module main-ns))
      output-exe)))

(defn compile-file [input-file main-ns output-exe]
  (compile-modules-to-exe
    (conj (map builder/build-lib-to-module builder/default-libs)
          (builder/build-asts-to-module main-ns
                                        (analyzer/analyze-file input-file))
          (builder/build-entry-module main-ns))
    output-exe
    {:dump     true
     :optimize true
     :verbose  true}))

(defn -main [input-file main-ns output-exe & args]
  (compile-file input-file main-ns output-exe)
  (println "Done!")
  (System/exit 0))
