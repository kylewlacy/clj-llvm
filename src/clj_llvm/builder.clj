(ns clj-llvm.builder
  (:require [clojure.pprint        :refer [pprint]]
            [clojure.java.io       :refer [reader]]
            [clj-llvm.llvm         :as    llvm]
            [clj-llvm.llvm.builder :as    builder]
            [clj-llvm.llvm.types   :as    types]
            [clj-llvm.runtime      :as    rt]
            [slingshot.slingshot   :refer [throw+]]))

(def ^:dynamic *globals*)
(def ^:dynamic *libs*)

(defmulti build-expr :op)
(defmulti build-const :type)

(declare globalize-fn)
(declare build-host-call)




(defmethod build-expr :do [{:keys [statements ret]}]
  (llvm/do- (map build-expr statements) (build-expr ret)))

(defmethod build-expr :const [ast]
  (build-const ast))

(defmethod build-expr :invoke [{*fn :fn args :args}]
  (apply llvm/invoke (build-expr *fn) (map build-expr args)))

; TODO: Multiple methods
(defmethod build-expr :fn [{:keys [methods]}]
  (build-expr (first methods)))

(defmethod build-expr :fn-method [{:keys [params body]}]
    (llvm/fn- (str (gensym "fn"))
              (types/FnType (repeat (count params) types/Int64)
                            types/Int64)
              :extern
              (build-expr body)))

; TODO: Locals other than args
(defmethod build-expr :local [{:keys [arg-id]}]
  (llvm/param arg-id))

(defmethod build-expr :def [{{name :name ns* :ns} :var init :init}]
  (let [init* (build-expr init)
        fn?  (= :fn-type (-> init* :type :kind))
        init (if fn? (globalize-fn init* name ns*)
                     init*)]
    (swap! *globals* assoc-in [ns* name] init)
    (if fn?
        init
        (llvm/set-global (str ns* "/" name)
                         (builder/return-type init)
                         init))))

(defmethod build-expr :var [{{name :name ns* :ns} :var}]
  (llvm/get-global (str ns* "/" name)))

; TODO: Pass meta to LLVM (somehow?)
(defmethod build-expr :with-meta [{:keys [expr]}]
  (build-expr expr))

(defmethod build-expr :host-interop [ast]
  (build-host-call (assoc ast :method (ast :m-or-f))))

(defmethod build-expr :host-call [ast]
  (build-host-call ast))

(defmethod build-expr :default [{:keys [op] :as ast}]
  (throw+
    {:type ::unkown-ast-node
     :node ast}
    (str "Don't know how to compile node of type " op)))



(defmethod build-const :number [{:keys [val]}]
  (llvm/const types/Int64 val))

(defmethod build-const :string [{:keys [val]}]
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

(defn build-host-call [{{lib :class} :target args :args method :method}]
  ; (:name fn-) could be simplified to just (str method), but
  ; we do it this way to ensure the method actually comes from
  ; where we expect it to come from
  (let [fn-  (get-in @*libs* [lib :globals method])
        args (map build-expr (or args []))]
    (apply llvm/invoke (llvm/get-fn (fn- :name))
                       args)))



(defn build-module* [main-ns & exprs]
  (apply llvm/module (gensym main-ns) (concat exprs
    [(llvm/fn- "main" (types/FnType [] types/Int64) :extern
      (llvm/do- []
        (llvm/invoke
          (llvm/get-fn ((get-in @*globals* [main-ns '-main]) :name)))))])))

(defn build-module [main-ns & asts]
  (with-bindings {#'*globals* (atom {})
                  #'*libs*    (atom {'clj-llvm.runtime rt/runtime-lib})}
    (builder/build-module
      (apply build-module* main-ns (concat (rt/runtime-lib :exprs)
                                         (mapv build-expr asts))))))
