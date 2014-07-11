(ns clj-llvm.compiler
  (:require [clojure.pprint        :refer [pprint]]
            [clojure.java.io       :refer [reader]]
            [clj-llvm.llvm         :as    llvm]
            [clj-llvm.llvm.builder :as   builder]
            [clj-llvm.llvm.types   :as    types]
            [clj-llvm.analyzer     :as    analyzer]
            [clj-llvm.runtime      :as    rt]))

(def ^:dynamic *globals*)
(def ^:dynamic *libs*)

(defmulti gen-expr :op)
(defmulti gen-const :type)

(declare globalize-fn)
(declare gen-host-call)

(defn dbg [& args] (apply println args) (last args))




(defmethod gen-expr :do [{:keys [statements ret]}]
  (llvm/do- (map gen-expr statements) (gen-expr ret)))

(defmethod gen-expr :const [ast]
  (gen-const ast))

(defmethod gen-expr :invoke [{*fn :fn args :args}]
  (llvm/invoke (gen-expr *fn) (map gen-expr args)))

; TODO: Multiple methods
(defmethod gen-expr :fn [{:keys [methods]}]
  (gen-expr (first methods)))

(defmethod gen-expr :fn-method [{:keys [params body]}]
    (llvm/fn- (str (gensym "fn"))
              (types/FnType (repeat (count params) types/Int64)
                            types/Int64)
              :extern
              (gen-expr body)))

; TODO: Locals other than args
(defmethod gen-expr :local [{:keys [arg-id]}]
  (llvm/param arg-id))

(defmethod gen-expr :def [{{name :name ns* :ns} :var init :init}]
  (let [init* (gen-expr init)
        fn?  (= :fn-type (-> init* :type :kind))
        init (if fn? (globalize-fn init* name ns*)
                     init*)]
    (swap! *globals* assoc-in [ns* name] init)
    (if fn?
        init
        (llvm/set-global (str ns* "/" name)
                         (builder/return-type init)
                         init))))

(defmethod gen-expr :var [{{name :name ns* :ns} :var}]
  (llvm/get-global (str ns* "/" name)))

; TODO: Pass meta to LLVM (somehow?)
(defmethod gen-expr :with-meta [{:keys [expr]}]
  (gen-expr expr))

(defmethod gen-expr :host-interop [ast]
  (gen-host-call (assoc ast :method (ast :m-or-f))))

(defmethod gen-expr :host-call [ast]
  (gen-host-call ast))

(defmethod gen-expr :default [ast]
  (println "Don't know how to gen AST:" ast))



(defmethod gen-const :number [{:keys [val]}]
  (llvm/const types/Int64 val))

(defmethod gen-const :string [{:keys [val]}]
  (let [char-vals (concat (mapv int val) [0])]
    (llvm/cast- (llvm/const (types/Array types/Int8 (count char-vals))
                            (mapv #(llvm/const types/Int8 %) char-vals))
                types/Int8*)))



(defn globalize-fn [fn- name ns-]
  (let [extern?    (-> name meta :extern)
        exact?     (-> name meta :exact)
        properties (merge (if extern? {:linkage :extern}
                                      {})
                          (if exact? {:name (str name)}
                                     {:name (str ns- "/" name)}))]
    (merge fn- properties)))

(defn gen-host-call [{{lib :class} :target args :args method :method}]
  ; (:name fn-) could be simplified to just (str method), but
  ; we do it this way to ensure the method actually comes from
  ; where we expect it to come from
  (let [fn-  (get-in @*libs* [lib :globals method])
        args (map gen-expr (or args []))]
    (llvm/invoke (llvm/get-fn (fn- :name))
                 args)))



(defn gen-module* [main-ns & exprs]
  (apply llvm/module (gensym main-ns) (concat exprs
    [(llvm/fn- "main" (types/FnType [] types/Int64) :extern
      (llvm/do- []
        (llvm/invoke
          (llvm/get-fn ((get-in @*globals* [main-ns '-main]) :name))
          [])))])))

; TODO: Garbage collection (link Boehm GC)
(defn gen-module [main-ns & asts]
  (with-bindings {#'*globals* (atom {})
                  #'*libs*    (atom {'clj-llvm.runtime rt/runtime-lib})}
    (builder/build-module
      (apply gen-module* main-ns (concat (rt/runtime-lib :exprs)
                                         (mapv gen-expr asts))))))

(defn maybe-dump [module options]
  (if (options :dump)
    (builder/dump module)
    module))

(defn maybe-optimize [module options]
  (if (options :optimize)
    (builder/optimize module)
    module))



(defn compile-module-to-file
  ([module output-exe]
    (compile-module-to-file module output-exe
      {:dump     false
       :optimize false}))
  ([module output-exe options]
    (-> module
        (maybe-dump options)
        builder/verify
        (maybe-optimize options)
        (builder/to-assembly-file (str output-exe ".s"))
        (builder/build-assembly-file output-exe))))

(defn compile-forms [forms main-ns output-exe]
  (compile-module-to-file
    (apply gen-module
           (symbol main-ns)
           (analyzer/analyze-forms forms
                                   (analyzer/empty-env)))
    output-exe))

(defn compile-file [input-file main-ns output-exe]
  (compile-module-to-file
    (apply gen-module
           (symbol main-ns)
           (analyzer/analyze-file input-file
                                  (analyzer/empty-env)))
    output-exe
    {:dump     true
     :optimize true}))

(defn -main [input-file main-ns output-exe & args]
  (compile-file input-file main-ns output-exe)
  (println "Done!"))
