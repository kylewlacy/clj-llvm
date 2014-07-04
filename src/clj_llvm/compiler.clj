(ns clj-llvm.compiler
  (:require [clojure.pprint       :refer [pprint]]
            [clojure.java.io      :refer [reader]]
            [mjolnir.constructors-init]
            [mjolnir.core         :as    mjolnir]
            [mjolnir.llvm-builder :as    builder]
            [mjolnir.types        :as    types]
            [mjolnir.expressions  :as    expr]
            [mjolnir.config       :as    config]
            [clj-llvm.analyzer    :as    analyzer]
            [clj-llvm.runtime     :as    rt])
  (:alias c mjolnir.constructors))

(def ^:dynamic *globals*)
(def ^:dynamic *libs*)

(defmulti gen-expr :op)
(defmulti gen-const :type)

(declare globalize-fn)
(declare gen-host-call)

(defn dbg [& args] (apply println args) (last args))




(defmethod gen-expr :do [{:keys [statements ret]}]
  (let [statements (if statements (concat statements [ret])
                                  [ret])]
    (apply c/do (map gen-expr statements))))

(defmethod gen-expr :const [ast]
  (gen-const ast))

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

(defmethod gen-expr :host-interop [ast]
  (gen-host-call (assoc ast :method (ast :m-or-f))))

(defmethod gen-expr :host-call [ast]
  (gen-host-call ast))

(defmethod gen-expr :default [ast]
  (println "Don't know how to gen AST:" ast))



(defmethod gen-const :number [{:keys [val]}]
  (c/const val -> types/Int64))

(defmethod gen-const :string [{:keys [val]}]
  (c/let [string (expr/->Malloc (types/->ArrayType types/Int8 (inc (count val))))]
    (apply c/do (concat
      (map
        #(c/aset string
                 (c/const % -> types/Int64)
                 (c/const (int (nth val %)) -> types/Int8))
        (range (count val)))
      [(c/aset string
               (c/const (inc (count val)) -> types/Int64)
               (c/const 0 -> types/Int8))]))
    (c/cast types/Int8* string)))



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
    (expr/->Call (expr/->Gbl (:name fn-))
                 args)))



(defn gen-module [main-ns & exprs]
  (apply c/module [] (concat exprs
    [(c/fn "main" (c/fn-t [] types/Int64) [] :extern
      (expr/->Call
        (expr/->Gbl (:name (get-in @*globals*
                                   [main-ns '-main])))
        []))])))

; TODO: Garbage collection (link Boehm GC)
(defn build-module [main-ns & asts]
  (with-bindings {#'*globals*           (atom {})
                  #'*libs*              (atom {
                    'clj-llvm.runtime rt/runtime-lib})
                  #'config/*int-type*   types/Int64
                  #'config/*float-type* types/Float64
                  #'config/*gc*         nil
                  #'config/*target*     (config/default-target)}
    (-> (apply gen-module main-ns (concat
          (map gen-expr asts)
          (rt/runtime-lib :exprs)))
        mjolnir/to-db
        (mjolnir/to-llvm-module true))))

(defn -main [input-file main-ns output-exe & args]
  (mjolnir/to-exe
    (apply build-module
      (symbol main-ns)
      (analyzer/analyze-file input-file
                             (analyzer/empty-env)))
    output-exe))
