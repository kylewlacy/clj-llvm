(ns clj-llvm.builder
  (:require [clojure.java.io              :refer [reader]]
            [clj-llvm.llvm                :as    llvm]
            [clj-llvm.llvm.module-builder :as    builder]
            [clj-llvm.llvm.types          :as    types]
            [clj-llvm.runtime             :as    rt]
            [slingshot.slingshot          :refer [throw+]]))

(def ^:dynamic *globals*)
(def ^:dynamic *locals*)
(def ^:dynamic *libs*)

(defmulti build-expr :op)
(defmulti build-const :type)

(declare globalize-fn
         build-host-interop
         build-host-call-fn
         build-host-struct-init
         build-host-struct-access
         build-block)




(defmethod build-expr :do [{:keys [statements ret]}]
  (let [statements (map build-expr (remove nil? (concat statements [ret])))]
    (apply llvm/doall- statements)))

(defmethod build-expr :const [ast]
  (build-const ast))

(defmethod build-expr :invoke [{*fn :fn args :args}]
  (apply llvm/call (build-expr *fn) (map build-expr args)))

; TODO: Multiple methods
(defmethod build-expr :fn [{:keys [methods]}]
  (build-expr (first methods)))

(defmethod build-expr :fn-method [{:keys [params body]}]
    (llvm/fn- (str (gensym "fn"))
              (types/FnType (repeat (count params) types/Int64)
                            types/Int64)
              :extern
              (build-block body)))

; TODO: Locals other than args
(defmethod build-expr :local [{:keys [arg-id name] :as ast}]
  (if arg-id
    (llvm/param arg-id)
    (@*locals* name)))

(defmethod build-expr :let [{:keys [bindings body]}]
  (binding [*locals* (atom {})]
    (let [statements       (concat bindings [body])
          built-statements (mapv build-expr statements)]
      (apply llvm/doall- built-statements))))

(defmethod build-expr :binding [{:keys [name init]}]
  (if-let [maybe-local (@*locals* name)]
    maybe-local
    (let [built-init  (build-expr init)
          type        (builder/return-type built-init)
          new-local   (llvm/alloca type (str name))
          store-local (llvm/store new-local built-init)
          load-local  (llvm/load- new-local)]
      (swap! *locals* assoc name load-local)
      store-local)))

(defmethod build-expr :def [{{name :name ns* :ns} :var init :init}]
  (let [built-init  (build-expr init)
        fn?         (= :fn-type (-> built-init :type :kind))
        built-init  (if fn?
                      (globalize-fn built-init name ns*)
                      built-init)
        global      (if fn?
                      built-init
                      (llvm/init-global (str ns* "/" name)
                                        (builder/return-type built-init)
                                        built-init))
        load-global (if fn?
                      global
                      (llvm/load- global))]
    (swap! *globals* assoc-in [ns* name] load-global)
    global))

(defmethod build-expr :var [{{name :name ns* :ns} :var :as ast}]
  (get-in @*globals* [ns* name]))

; TODO: Pass meta to LLVM (somehow?)
(defmethod build-expr :with-meta [{:keys [expr]}]
  (build-expr expr))

(defmethod build-expr :host-interop [ast]
  (build-host-interop (assoc ast :method (ast :m-or-f))))

(defmethod build-expr :host-call [ast]
  (build-host-interop ast))

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

(defn build-host-call-fn [{{lib :class} :target args :args method :method}]
  ; TODO: Throw error if fn- is nil
  (let [fn-  (get-in @*libs* [lib :globals method])
        args (map build-expr (or args []))]
    (apply llvm/call fn- args)))

(defn build-host-struct-init [{:keys [target args method] :as ast}]
  (let [lib          (target :class)
        struct-type  (get-in @*libs* [lib :globals method])
        args         (map build-expr (or args []))
        struct       (llvm/alloca struct-type)
        member-types (struct-type :member-types)
        members      (map #(llvm/get-element-ptr-in-bounds
                            struct
                            (llvm/const types/Int32 0)
                            (llvm/const types/Int32 %))
                          (range 0 (count member-types)))
        casts        (map llvm/cast- args member-types)]
    (apply llvm/doall- (concat (map llvm/store members casts)
                               [struct]))))

(defn build-host-struct-access [{:keys [target args method] :as ast}]
  (let [args          (map build-expr (or args []))
        struct        (build-expr target)
        type          (builder/return-type struct)
        idx           (get-in type [:el-type :idx method])]
    (llvm/load-
      (llvm/get-element-ptr-in-bounds
        struct
        (llvm/const types/Int32 0)
        (llvm/const types/Int32 idx)))))

(defn build-host-interop [{:keys [target args method] :as ast}]
  (let [lib           (target :class)
        callee        (get-in @*libs* [lib :globals method])
        build-struct? (= :struct-type (get callee :kind))
        get-member?   (= :local (target :op))] ; TODO: Other than local
    (cond
      build-struct? (build-host-struct-init ast)
      get-member?   (build-host-struct-access ast)
      :else         (build-host-call-fn ast))))

(defn build-block [{:keys [statements ret]}]
  (let [ret        (llvm/ret (if ret (build-expr ret) nil))
        statements (concat (map build-expr (remove nil? statements)) [ret])]
    (apply llvm/block statements)))



(defn build-module* [main-ns exprs]
  (apply llvm/module (gensym main-ns) (concat exprs
    [(llvm/fn- "main" (types/FnType [] types/Int64) :extern
      (llvm/block
        (llvm/ret
          (llvm/call (get-in @*globals* [main-ns '-main])))))])))

(defn build-module
  ([main-ns asts]
    (build-module main-ns asts nil))
  ([main-ns asts libs]
    (binding [*globals* (atom {})
              *libs*    (atom (apply hash-map
                                     (mapcat #(vector (% :name) %)
                                             (concat [rt/runtime-lib] libs))))]

      (builder/build-module
        (build-module* main-ns
                       (flatten (concat (mapcat :exprs (vals @*libs*))
                                        (mapv build-expr asts))))))))
