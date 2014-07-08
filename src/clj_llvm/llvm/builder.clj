(ns clj-llvm.llvm.builder
  (:require [clj-llvm.llvm.native :as native]))

(def ^:dynamic *builder*)
(def ^:dynamic *module*)
(def ^:dynamic *current-fn*)
(def ^:dynamic *globals*)
(def ^:dynamic *fns*)



(defmulti build-expr :op)
(defmulti build-type :kind)
(defmulti build-const #(-> % :type :kind))
(defmulti return-type :op)



(def kw->cast-fn
  {:bitcast {:const native/LLVMConstBitCast
             :vary  native/LLVMBuildBitCast}
   :trunc   {:const native/LLVMConstTrunc
             :vary  native/LLVMBuildTrunc}
   :zext    {:const native/LLVMConstZExt
             :vary  native/LLVMBuildZExt}})

(defn get-casting-types [{:keys [expr to-type] :as ast}]
  [(return-type expr) to-type])

(defn get-casting-kinds [ast]
  (map :kind (get-casting-types ast)))

(defmulti cast-kind* (fn [ast types kinds] (get-casting-kinds ast)))

(defn cast-kind [ast]
  (let [types (get-casting-types ast)
        kinds (map :kind types)]
    (cast-kind* ast types kinds)))

(defn build-cast [{:keys [expr to-type] :as ast}]
  (let [cast-kind  (cast-kind ast)
        cast-fn    (kw->cast-fn cast-kind)
        built-expr (build-expr expr)]
    (if cast-kind
      (do
        (native/LLVMTypeOf built-expr) ; Forces the expression's
                                       ; type to be initialized
                                       ; TODO: Can we do this by hand?
        ((cast-fn :vary) *builder*
                         built-expr
                         (build-expr to-type)
                         (str (gensym "cast"))))
      (do
        (println "NOTE: expression" ast "couldn't be casted; using as-is")
        built-expr))))



(defmethod build-expr :cast [ast]
  (build-cast ast))

(defmethod build-expr :const [ast]
  (build-const ast))

(defmethod build-expr :do [{:keys [statements ret] :as ast}]
  (let [block (native/LLVMAppendBasicBlock (*current-fn* :expr)
                                           (str (gensym "do")))
        builder (native/LLVMCreateBuilder)]
    (native/LLVMPositionBuilderAtEnd builder block)
    (binding [*builder* builder]
      (when statements
        (doseq [statement statements]
          (build-expr statement)))
      (if ret
        (native/LLVMBuildRet *builder* (build-expr ret))
        (native/LLVMBuildRetVoid *builder*)))
    block))

(defmethod build-expr :fn [{:keys [name type linkage body] :as ast}]
  ; TODO: Use linkage
  (let [fn-    (native/LLVMAddFunction *module* name (build-expr type))
        fn-ref (native/LLVMGetNamedFunction *module* name)]
    (native/LLVMSetFunctionCallConv fn- native/LLVMCCallConv)
    (native/LLVMSetLinkage fn- native/LLVMExternalLinkage)
    (if body
      (binding [*current-fn* {:expr fn- :ast ast :ref fn-ref}]
        (build-expr body)))
    (swap! *fns* assoc name ast)
    fn-))

(defmethod build-expr :get-fn [{:keys [name]}]
  (native/LLVMGetNamedFunction *module* (str name)))

(defmethod build-expr :get-global [{:keys [name]}]
  (native/LLVMGetNamedGlobal *module* (str name)))

(defmethod build-expr :invoke [ast]
  (let [fn-       (ast :fn)
        args      (ast :args)
        ret-void? (= :void (-> ast return-type :kind))
        name      (if ret-void? "" (str (gensym "invoke")))
        arg-ptr   (native/map-parr build-expr args)]
    (native/LLVMBuildCall *builder*
                          (build-expr fn-)
                          arg-ptr
                          (count args)
                          name)))

(defmethod build-expr :param [{idx :idx}]
  (native/LLVMGetParam (*current-fn* :expr) idx))

(defmethod build-expr :set-global [{:keys [name type init] :as ast}]
  (swap! *globals* assoc name ast)
  (let [global (native/LLVMAddGlobal *module* (build-expr type) name)]
    (native/LLVMSetInitializer global init)
    global))

(defmethod build-expr :type [ast]
  (build-type ast))

(defmethod build-expr :default [ast]
  (println "Don't know how to build expression from" ast))



(defmethod build-type :fn-type [{:keys [arg-types ret-type variadic?] :as ast}]
  (native/LLVMFunctionType (build-expr ret-type)
                           (native/map-parr build-expr arg-types)
                           (count arg-types)
                           variadic?))

(defmethod build-type :int [{:keys [width]}]
  (native/LLVMIntType width))

(defmethod build-type :pointer [{:keys [el-type]}]
  (native/LLVMPointerType (build-expr el-type) 0))

(defmethod build-type :array [{:keys [el-type count]}]
  (native/LLVMArrayType (build-expr el-type) count))

(defmethod build-type :void [ast]
  (native/LLVMVoidType))

(defmethod build-type :default [ast]
  (println "Don't know how to build type from" ast))



; TODO: Signed/unsigned
(defmethod build-const :int [{:keys [type val]}]
  (native/LLVMConstInt (build-expr type) val true))

(defmethod build-const :pointer [{:keys [type val] :as ast}]
  (if (nil? val)
    (native/LLVMConstNull (build-expr type))
    (println "Can't build non-nil pointer from" ast)))

(defmethod build-const :array [{type :type val :val}]
  (let [el-type (type :el-type)
        global  (native/LLVMAddGlobal *module*
                                      (build-expr type)
                                      (str (gensym "str")))
        const   (native/LLVMConstArray (build-expr el-type)
                                       (native/map-parr build-expr val)
                                       (count val))]
    (native/LLVMSetInitializer global const)
    global))

(defmethod build-const :default [ast]
  (println "Don't know how to build const from" ast))



(defmethod return-type :cast [{:keys [to-type]}]
  to-type)

(defmethod return-type :const [{:keys [type]}]
  type)

(defmethod return-type :do [{:keys [ret]}]
  (return-type ret))

(defmethod return-type :fn [{:keys [type]}]
  (:ret-type type))

(defmethod return-type :get-fn [{:keys [name]}]
  (-> @*fns* (get name) return-type))

(defmethod return-type :get-global [{:keys [name]}]
  (-> @*globals* (get name) return-type))

(defmethod return-type :invoke [{fn- :fn}]
  (return-type fn-))

(defmethod return-type :param [{:keys [idx]}]
  (nth (-> *current-fn* :ast :type :arg-types) idx))

(defmethod return-type :type [type]
  type)



(defmethod cast-kind* [:int :int] [ast [from-type to-type] kinds]
  (cond
    (< (:width from-type) (:width to-type)) :zext
    (> (:width from-type) (:width to-type)) :trunc
    :else                                   false))

(defmethod cast-kind* [:array :pointer] [ast types kinds]
  :bitcast)

(defmethod cast-kind* :default [ast types kinds]
  (println "Don't know how to make cast from kinds" kinds))



(defn build-module [{:keys [name exprs]}]
  (let [module (native/LLVMModuleCreateWithName (str name))]
    (binding [*module* module
              *globals* (atom {})
              *fns* (atom {})]
      (doseq [expr exprs]
        (build-expr expr)))
    module))

(defn find-llvm-target-by-name [name]
  (first (filter (comp (partial = name) :name)
                 (native/target-seq))))

(defn create-target-machine []
  (native/LLVMInitializeX86TargetInfo)
  (native/LLVMInitializeX86Target)
  (native/LLVMInitializeX86TargetMC)
  (native/LLVMInitializeX86AsmPrinter)
  (native/LLVMInitializeX86AsmParser)
  (native/LLVMCreateTargetMachine ((find-llvm-target-by-name "x86-64") :target)
                                  "x86_64-apple-darwin13.2.0"
                                  "generic"
                                  ""
                                  native/LLVMCodeGenLevelDefault
                                  native/LLVMRelocDefault
                                  native/LLVMCodeModelDefault))

(defn verify [module]
  (let [error-message (native/new-pointer)
        error?        (native/LLVMVerifyModule module
                                               native/LLVMPrintMessageAction
                                               error-message)]
    (assert (not error?) (.getString (native/value-at error-message) 0))

    (native/LLVMDisposeMessage (native/value-at error-message)))
  module)

(defn add-default-passes [pass-manager]
  (doto pass-manager
    native/LLVMAddFunctionInliningPass
    native/LLVMAddLoopUnrollPass
    native/LLVMAddGVNPass
    native/LLVMAddCFGSimplificationPass
    native/LLVMAddBBVectorizePass
    native/LLVMAddConstantPropagationPass
    native/LLVMAddInstructionCombiningPass
    native/LLVMAddPromoteMemoryToRegisterPass))

(defn dump [module]
  (native/LLVMDumpModule module)
  module)

(defn optimize [module]
  (let [pass-manager (native/LLVMCreatePassManager)]
    (add-default-passes pass-manager)
    (native/LLVMRunPassManager pass-manager module)
    (native/LLVMDisposePassManager pass-manager))
  (native/LLVMDumpModule module)
  module)

(defn to-assembly-file [module file]
  (println "Emitting assembly" file)
  (let [target (create-target-machine)
        err    (native/new-pointer)]
    (when (native/LLVMTargetMachineEmitToFile target
                                              module
                                              file
                                              native/LLVMAssemblyFile
                                              err)
      (assert false (.getString (native/value-at err) 0))))
  file)

(defn build-assembly-file [assembly-file exe-file]
  (println "Building")
  (clojure.java.shell/sh "cc" assembly-file "-o" exe-file))
