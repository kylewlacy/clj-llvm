(ns cljl.compiler
  (:require [clojure.string           :as    str]
            [clojure.java.io          :refer [reader]]
            [cljl.analyzer            :as    analyzer]
            [cljl.llvm.module-builder :as    module-builder]
            [cljl.builder             :as    builder]))

(defn maybe-dump [module options]
  (when (options :dump)
    (let [ir    (module-builder/module-to-ir-string module)
          lines (str/split-lines ir)]
      (println (str/join "\n" (map #(str "  " %) lines)))))
  module)

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

(defn module-to-object [module output-file options]
  (when (options :verbose)
    (println "Writing object file...")
    (println (str "  " output-file)))
  (module-builder/module-to-object module output-file))

(defn objects-to-executable [objects output-exe options]
  (when (options :verbose)
    (println "Compiling objects to executable...")
    (doseq [object objects]
      (println (str "  " object)))
    (println "  =>" output-exe))
  (module-builder/objects-to-executable objects output-exe))





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
        (module-to-object (get-temp-filename) options))))

(defn compile-modules-to-exe
  ([modules output-exe]
    (compile-modules-to-exe modules output-exe {:dump     false
                                                :optimize false
                                                :verbose  false}))
  ([modules output-exe options]
    (objects-to-executable
      (mapv compile-module-to-object modules (repeat options))
      output-exe
      options)))

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
