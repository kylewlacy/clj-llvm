(ns cljl.llvm.module-builder
  (:require [clojure.java.shell  :as    shell]
            [cljl.llvm           :as    llvm]
            [cljl.llvm.types     :as    types]
            [cljl.llvm.interop   :as    interop]
            [cljl.native-interop :refer [map-parr
                                         new-pointer
                                         value-at]]))

(def ^:dynamic *builder*)
(def ^:dynamic *module*)
(def ^:dynamic *current-fn*)
(def ^:dynamic *globals*)
(def ^:dynamic *fns*)
(def ^:dynamic *built-values*)
; *built-values* contains a map from unique ID's to the raw LLVM type. The
; reason for this is because, in our LLVM AST, two identical nodes are meant
; to refer to the same object. For example, :call's :fn field is actually
; the :fn node that is being invoked; naïevely rebuilding this node would
; create a new function, when we really want the originally built version



(defmulti build-expr :llvm/op)
(defmulti build-type :kind)
(defmulti build-const #(-> % :type :kind))
(defmulti return-type :llvm/op)



(def kw->cast-fn
  {:bitcast interop/LLVMBuildBitCast
   :trunc   interop/LLVMBuildTrunc
   :zext    interop/LLVMBuildZExt})

(defn get-casting-types [{:keys [expr to-type] :as ast}]
  [(return-type expr) to-type])

(defn get-casting-kinds [ast]
  (map :kind (get-casting-types ast)))

(defmulti cast-kind* (fn [ast types kinds] (get-casting-kinds ast)))

(defn cast-kind [ast]
  (let [types (get-casting-types ast)
        kinds (mapv :kind types)]
    (cast-kind* ast types kinds)))

(defn build-cast [{:keys [expr to-type] :as ast}]
  (let [cast-kind  (cast-kind ast)
        cast-fn    (kw->cast-fn cast-kind)
        built-expr (build-expr expr)]
    (if cast-kind
      (do
        (interop/LLVMTypeOf built-expr) ; Forces the expression's
                                        ; type to be initialized
                                        ; TODO: Can we do this by hand?
        (cast-fn *builder*
                  built-expr
                  (build-expr to-type)
                  (str (gensym "cast"))))
      built-expr)))



(defmethod build-expr :alloca [{:keys [type name id]}]
  (if-let [maybe-alloca (@*built-values* id)]
    maybe-alloca
    (let [new-alloca (interop/LLVMBuildAlloca *builder*
                                              (build-expr type)
                                              name)]
      (swap! *built-values* assoc id new-alloca)
      new-alloca)))

(defmethod build-expr :block [{:keys [statements] :as ast}]
  (let [block (interop/LLVMAppendBasicBlock (*current-fn* :expr)
                                            (str (gensym "block")))
        builder (interop/LLVMCreateBuilder)]
    (interop/LLVMPositionBuilderAtEnd builder block)
    (binding [*builder* builder]
      (if statements
        (doseq [statement statements]
          (build-expr statement))))
    block))

(defmethod build-expr :cast [ast]
  (build-cast ast))

(defmethod build-expr :const [ast]
  (build-const ast))

(defmethod build-expr :doall [{:keys [statements ret]}]
  (if statements
    (doseq [statement statements]
      (build-expr statement)))
  (build-expr ret))

(defmethod build-expr :fn [{:keys [name id type linkage body] :as ast}]
  ; TODO: Use linkage
  (if-let [maybe-fn (@*built-values* id)]
    maybe-fn
    (let [new-fn (interop/LLVMAddFunction *module* name (build-expr type))]
      (interop/LLVMSetFunctionCallConv new-fn interop/LLVMCCallConv)
      (interop/LLVMSetLinkage new-fn interop/LLVMExternalLinkage)
      (if body
        (binding [*current-fn* {:expr new-fn :ast ast}]
          (build-expr body)))
      (swap! *fns* assoc name ast)
      (swap! *built-values* assoc id new-fn)
      new-fn)))

(defmethod build-expr :get-fn [{:keys [name]}]
  (interop/LLVMGetNamedFunction *module* (str name)))

(defmethod build-expr :get-global [{:keys [name]}]
  (interop/LLVMGetNamedGlobal *module* (str name)))

(defmethod build-expr :call [{fn- :fn args :args :as ast}]
  (let [ret-void? (= :void (-> ast return-type :kind))
        name      (if ret-void? "" (str (gensym "invoke")))
        arg-ptr   (map-parr build-expr args)]
    (interop/LLVMBuildCall *builder*
                           (build-expr fn-)
                           arg-ptr
                           (count args)
                           name)))

(defmethod build-expr :load [{var- :var}]
  (interop/LLVMBuildLoad *builder*
                         (build-expr var-)
                         (or (var- :name) (str (gensym "load")))))

(defmethod build-expr :param [{:keys [idx]}]
  (interop/LLVMGetParam (*current-fn* :expr) idx))

(defmethod build-expr :ret [{:keys [val]}]
  (if val
    (interop/LLVMBuildRet *builder* (build-expr val))
    (interop/LLVMBuildRetVoid *builder*)))

(defmethod build-expr :init-global [{:keys [name type id val] :as ast}]
  (if-let [maybe-global (@*built-values* id)]
    maybe-global
    (let [global (interop/LLVMAddGlobal *module* (build-expr type) name)]
      (swap! *globals* assoc name ast)
      (if val (interop/LLVMSetInitializer global (build-expr val)))
      (swap! *built-values* assoc id global)
      global)))

(defmethod build-expr :store [{:keys [var val]}]
  (interop/LLVMBuildStore *builder* (build-expr val) (build-expr var)))

(defmethod build-expr :get-element-ptr [{:keys [pointer idx in-bounds?]}]
  (let [build-fn (if in-bounds?
                   interop/LLVMBuildInBoundsGEP
                   interop/LLVMBuildGEP)]
    (build-fn *builder*
              (build-expr pointer)
              (map-parr build-expr idx)
              (count idx)
              (str (gensym "gep")))))

(defmethod build-expr :type [ast]
  (build-type ast))

(defmethod build-expr nil [ast]
  (throw (ex-info (str "Can't build LLVM expression from non-LLVM node")
                  {:type ::unknown-expr
                   :node ast})))

(defmethod build-expr :default [{op :llvm/op :as ast}]
  (throw (ex-info (str "Don't know how to build LLVM expression of type" op)
                  {:type ::unknown-expr
                   :node ast})))



(defmethod build-type :fn-type [{:keys [arg-types ret-type variadic?]}]
  (interop/LLVMFunctionType (build-expr ret-type)
                            (map-parr build-expr arg-types)
                            (count arg-types)
                            variadic?))

(defmethod build-type :struct-type [{:keys [member-types]}]
  (let [struct-type (interop/LLVMStructType (map-parr build-expr member-types)
                                            (count member-types)
                                            false)]
    struct-type))

(defmethod build-type :int [{:keys [width]}]
  (interop/LLVMIntType width))

(defmethod build-type :pointer [{:keys [el-type]}]
  (interop/LLVMPointerType (build-expr el-type) 0))

(defmethod build-type :array [{:keys [el-type count]}]
  (interop/LLVMArrayType (build-expr el-type) count))

(defmethod build-type :void [_]
  (interop/LLVMVoidType))

(defmethod build-type nil [ast]
  (throw (ex-info (str "Can't build non-LLVM type from non-LLVM node")
                  {:type ::unknown-type
                   :node ast})))

(defmethod build-type :default [{:keys [kind] :as ast}]
  (throw (ex-info (str "Don't know how to build LLVM type of kind " kind)
                  {:type ::unknown-type
                   :node ast})))



; TODO: Signed/unsigned
(defmethod build-const :int [{:keys [type val]}]
  (interop/LLVMConstInt (build-expr type) val true))

(defmethod build-const :pointer [{:keys [type val] :as ast}]
  (if (nil? val)
    (interop/LLVMConstNull (build-expr type))
    (throw (ex-info (str "Can't build non-nil LLVM pointer of type kind "
                         (type :kind))
                    {:type ::non-nil-const-pointer
                     :node ast}))))

(defmethod build-const :array [{:keys [type val]}]
  (let [el-type (type :el-type)
        global  (interop/LLVMAddGlobal *module*
                                       (build-expr type)
                                       (str (gensym "str")))
        const   (interop/LLVMConstArray (build-expr el-type)
                                        (map-parr build-expr val)
                                        (count val))]
    (interop/LLVMSetInitializer global const)
    global))

(defmethod build-const nil [ast]
  (throw (ex-info (str "Can't build LLVM const from non-const node")
                  {:type ::unknown-const
                   :node ast})))

(defmethod build-const :default [{{kind :kind} :type :as ast}]
  (throw (ex-info (str "Don't know how to build LLVM const of type kind " kind)
                  {:type ::unknown-const
                   :node ast})))



(defmethod return-type :alloca [{:keys [type]}]
  (types/Pointer type))

(defmethod return-type :cast [{:keys [to-type]}]
  to-type)

(defmethod return-type :const [{:keys [type]}]
  type)

(defmethod return-type :doall [{:keys [ret]}]
  (return-type ret))

(defmethod return-type :fn [{:keys [type]}]
  (:ret-type type))

(defmethod return-type :get-fn [{:keys [name]}]
  (-> @*fns* (get name) return-type))

(defmethod return-type :get-global [{:keys [name]}]
  (-> @*globals* (get name) return-type))

(defmethod return-type :call [{fn- :fn :as ast}]
  (return-type fn-))

(defmethod return-type :param [{:keys [idx]}]
  (nth (-> *current-fn* :ast :type :arg-types) idx))

(defmethod return-type :load [{var- :var}]
  ((return-type var-) :el-type))

(defmethod return-type :type [type]
  type)

(defmethod return-type nil [ast]
  (throw (ex-info (str "Can't get return type from non-LLVM node")
                  {:type ::unknown-return-type
                   :node ast})))

(defmethod return-type :default [{op :llvm/op :as ast}]
  (throw (ex-info (str "Don't know how to get return type for node " op)
                  {:type ::unknown-return-type
                   :node ast})))



(defmethod cast-kind* [:int :int] [ast [from-type to-type] kinds]
  (cond
    (< (:width from-type) (:width to-type)) :zext
    (> (:width from-type) (:width to-type)) :trunc
    :else                                   false))

(defmethod cast-kind* [:array :pointer] [ast types kinds]
  :bitcast)

(defmethod cast-kind* [:pointer :pointer] [ast types kinds]
  :bitcast)

(defmethod cast-kind* nil [ast types kinds]
  (throw (ex-info (str "Can't build cast from non-LLVM node " ast)
                  {:type  ::unknown-cast
                   :node  ast
                   :types types
                   :kinds kinds})))

(defmethod cast-kind* :default [ast types kinds]
  (throw (ex-info (str "Don't know how to build cast from kinds " kinds)
                  {:type  ::unknown-cast
                   :node  ast
                   :types types
                   :kinds kinds})))



(defn build-module [{:keys [name exprs]}]
  (let [module (interop/LLVMModuleCreateWithName (str name))]
    (binding [*module*       module
              *globals*      (atom {})
              *fns*          (atom {})
              *built-values* (atom {})]
      (doseq [expr exprs]
        (build-expr expr)))
    module))

(defn find-llvm-target-by-name [name]
  (first (filter (comp (partial = name) :name)
                 (interop/target-seq))))

(defn create-target-machine []
  (interop/LLVMInitializeX86TargetInfo)
  (interop/LLVMInitializeX86Target)
  (interop/LLVMInitializeX86TargetMC)
  (interop/LLVMInitializeX86AsmPrinter)
  (interop/LLVMInitializeX86AsmParser)
  (interop/LLVMCreateTargetMachine (-> (interop/target-seq) first :target)
                                   (interop/LLVMGetDefaultTargetTriple)
                                   "generic"
                                   ""
                                  interop/LLVMCodeGenLevelDefault
                                  interop/LLVMRelocDefault
                                  interop/LLVMCodeModelDefault))

(defn verify [module]
  (let [error-message (new-pointer)
        error?        (interop/LLVMVerifyModule module
                                                interop/LLVMPrintMessageAction
                                                error-message)]
    (assert (not error?) (.getString (value-at error-message) 0))
    (interop/LLVMDisposeMessage (value-at error-message)))
  module)

(defn add-default-passes [pass-manager]
  (doto pass-manager
    interop/LLVMAddFunctionInliningPass
    interop/LLVMAddLoopUnrollPass
    interop/LLVMAddGVNPass
    interop/LLVMAddCFGSimplificationPass
    interop/LLVMAddBBVectorizePass
    interop/LLVMAddConstantPropagationPass
    interop/LLVMAddInstructionCombiningPass
    interop/LLVMAddPromoteMemoryToRegisterPass))

(defn dump [module]
  (interop/LLVMDumpModule module)
  module)

(defn module-to-ir-string [module]
  (let [ir  (interop/LLVMPrintModuleToString module)
        str (.getString ir 0)]
    (interop/LLVMDisposeMessage ir)
    str))

(defn optimize [module]
  (let [pass-manager (interop/LLVMCreatePassManager)]
    (add-default-passes pass-manager)
    (interop/LLVMRunPassManager pass-manager module)
    (interop/LLVMDisposePassManager pass-manager))
  module)

(def kw->file-type
  {:assembly-file interop/LLVMAssemblyFile
   :object-file   interop/LLVMObjectFile})

(defn module-to-file-type [module file-type output-file]
  (let [target        (create-target-machine)
        output-type   (kw->file-type file-type)
        error-message (new-pointer)
        error?        (interop/LLVMTargetMachineEmitToFile target
                                                           module
                                                           output-file
                                                           output-type
                                                           error-message)]
      (assert (not error?) (.getString (value-at error-message) 0)))
  output-file)

(defn module-to-object [module output-file]
  (module-to-file-type module :object-file output-file))

(defn module-to-assembly [module output-file]
  (module-to-file-type module :assembly-file output-file))

(defn objects-to-lib [object-files lib-file]
  (let [command (concat ["gcc" "-dynamiclib" "-o" lib-file] object-files)
        result (apply shell/sh command)]
    (if (not= (result :exit) 0)
      (throw (ex-info (str "Compiling objects to lib failed!\n" (result :err))
                      {:type    ::build-failed
                       :objects object-files
                       :lib     lib-file
                       :result  result})))))

(defn objects-to-executable [object-files exe-file]
  (let [command (concat ["gcc" "-o" exe-file] object-files)
        result (apply shell/sh command)]
    (if (not= (result :exit) 0)
      (throw (ex-info (str "Compiling objects to exe failed!\n" (result :err))
                      {:type    ::build-failed
                       :objects object-files
                       :exe     exe-file
                       :result  result}))))
  exe-file)

(defn assembly-to-executable [assembly-file exe-file]
  (let [result (shell/sh "cc" assembly-file "-o" exe-file)]
    (if (not= (result :exit) 0)
      (throw (ex-info (str "Compiling assembly to exe failed!\n" (result :err))
                      {:type     ::build-failed
                       :assembly assembly-file
                       :exe      exe-file
                       :result   result})))))
