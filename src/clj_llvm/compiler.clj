(ns clj-llvm.compiler
  (:require [clojure.pprint       :refer [pprint]]
            [clojure.java.io      :refer [reader]]
            [mjolnir.constructors-init]
            [mjolnir.core         :as    mjolnir]
            [mjolnir.llvm-builder :as    builder]
            [mjolnir.types        :as    types]
            [mjolnir.expressions  :as    expr]
            [mjolnir.config       :as    config]
            [clj-llvm.analyzer    :as    analyzer])
  (:alias c mjolnir.constructors))

(def ^:dynamic *globals*)



(defn dbg [& args] (apply println args) (last args))

(defmulti gen-expr :op)

(defmethod gen-expr :do [{:keys [statements ret]}]
  (let [statements (if statements (concat statements [ret])
                                  [ret])]
    (apply c/do (map gen-expr statements))))

(defmethod gen-expr :const [{:keys [val]}]
  (c/const val -> types/Int64))

(defmethod gen-expr :invoke [{*fn :fn args :args}]
  (expr/->Call (gen-expr *fn) (mapv gen-expr args)))

; TODO: Multiple methods
(defmethod gen-expr :fn [{:keys [methods]}]
  (gen-expr (first methods)))

(defmethod gen-expr :fn-method [{:keys [params body]}]
    (expr/map->Fn
      {:type      (c/fn-t (repeat (count params) types/Int64)
                          types/Int64)
       :name      (str (gensym "fn"))
       :arg-names (mapv #(-> % :name str) params)
       :body      (gen-expr body)}))

; TODO: Locals other than args
(defmethod gen-expr :local [{:keys [arg-id]}]
  (expr/->Arg arg-id))

(defn globalize-fn [fn- name ns-]
  (let [extern?    (-> name meta (#(dbg "meta" %)) :extern)
        exact?     (-> name meta :exact)
        properties (merge (if extern? {:linkage :extern}
                                      {})
                          (if exact? {:name (str name)}
                                     {:name (str ns- "/" name)}))]
    (merge fn- (dbg "  properties" properties))))

(defmethod gen-expr :def [{{name :name ns* :ns} :var init :init}]
  (let [init* (gen-expr init)
        fn?  (types/FunctionType? (:type init*))
        init (if fn? (globalize-fn init* name ns*)
                     init*)]
    (swap! *globals* assoc-in [ns* name] init)
    (if fn?
        init
        (c/global (str ns* "/" name) (:type init) init))))

(defmethod gen-expr :var [{{name :name ns* :ns} :var}]
  (expr/->Gbl (str ns* "/" name)))

; TODO: Pass meta to LLVM (somehow?)
(defmethod gen-expr :with-meta [{:keys [expr]}]
  (gen-expr expr))

(defmethod gen-expr :default [ast]
  (println "Don't know how to gen AST:" ast))



(defn gen-module [main-ns & exprs]
  (apply c/module [] (concat exprs
    [(c/fn "main" (c/fn-t [] types/Int64) [] :extern
      (expr/->Call
        (expr/->Gbl (:name (get-in @*globals*
                                   [main-ns '-main])))
        []))])))

; TODO: Garbage collection (link Boehm GC)
(defn build-module [main-ns & asts]
  (with-bindings {#'*globals* (atom {})
                  #'config/*int-type* types/Int64
                  #'config/*float-type* types/Float64
                  #'config/*gc* nil
                  #'config/*target* (config/default-target)}
    (-> (apply gen-module main-ns (map gen-expr asts))
        mjolnir/to-db
        (mjolnir/to-llvm-module true))))

(defn -main [input-file main-ns output-exe & args]
  (mjolnir/to-exe
    (apply build-module
      (symbol main-ns)
      (analyzer/analyze-file input-file
                             (analyzer/empty-env)))
    output-exe))



(comment
  ; Sample file emission
  (mjolnir.targets.target/emit-to-file
    (mjolnir.config/default-target)
    (:module (build-module 'user (analyzer/analyze
                '(def -main (fn* -main [] 2)))))
    {:output-type :asm})
)
