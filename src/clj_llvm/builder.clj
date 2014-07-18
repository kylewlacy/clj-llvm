(ns clj-llvm.builder
  (:require [clojure.java.io              :refer [reader]]
            [clj-llvm.llvm                :as    llvm]
            [clj-llvm.llvm.module-builder :as    builder]
            [clj-llvm.llvm.types          :as    types]
            [clj-llvm.runtime             :as    rt]
            [slingshot.slingshot          :refer [throw+]]))

(def ^:dynamic *globals*)
(def ^:dynamic *libs*)

(defmulti build-expr :op)
(defmulti build-const :type)

(declare globalize-fn)
(declare build-host-call)




(defmethod build-expr :do [{:keys [statements ret body?]}]
  (if body?
    (let [ret        (llvm/ret (if ret (build-expr ret) nil))
          statements (concat (map build-expr (remove nil? statements)) [ret])]
      (apply llvm/block statements))
    (let [statements (map build-expr (remove nil? (concat statements [ret])))]
      (apply llvm/doall- statements))))

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

(defmethod build-expr :var [{{name :name ns* :ns} :var :as ast}]
  (get-in @*globals* [ns* name]))

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
  (let [fn-  (get-in @*libs* [lib :globals method])
        args (map build-expr (or args []))]
    (apply llvm/invoke fn- args)))



(defn build-module* [main-ns & exprs]
  (apply llvm/module (gensym main-ns) (concat exprs
    [(llvm/fn- "main" (types/FnType [] types/Int64) :extern
      (llvm/block
        (llvm/ret
          (llvm/invoke (get-in @*globals* [main-ns '-main])))))])))

(defn build-module [main-ns & asts]
  (with-bindings {#'*globals* (atom {})
                  #'*libs*    (atom {'clj-llvm.runtime rt/runtime-lib})}
    (builder/build-module
      (apply build-module* main-ns (concat (rt/runtime-lib :exprs)
                                           (mapv build-expr asts))))))
