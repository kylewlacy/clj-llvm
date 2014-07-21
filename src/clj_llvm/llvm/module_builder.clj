(ns clj-llvm.llvm.module-builder
  (:require [clojure.java.shell   :as    shell]
            [slingshot.slingshot  :refer [throw+]]
            [clj-llvm.llvm        :as    llvm]
            [clj-llvm.llvm.types  :as    types]
            [clj-llvm.llvm.native :as    native]))

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



(defmulti build-expr :op)
(defmulti build-type :kind)
(defmulti build-const #(-> % :type :kind))
(defmulti return-type :op)



(def kw->cast-fn
  {:bitcast native/LLVMBuildBitCast
   :trunc   native/LLVMBuildTrunc
   :zext    native/LLVMBuildZExt})

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
        (native/LLVMTypeOf built-expr) ; Forces the expression's
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
    (let [new-alloca (native/LLVMBuildAlloca *builder* (build-expr type) name)]
      (swap! *built-values* assoc id new-alloca)
      new-alloca)))

(defmethod build-expr :block [{:keys [statements] :as ast}]
  (let [block (native/LLVMAppendBasicBlock (*current-fn* :expr)
                                           (str (gensym "block")))
        builder (native/LLVMCreateBuilder)]
    (native/LLVMPositionBuilderAtEnd builder block)
    (binding [*builder*      builder]
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
    (let [new-fn (native/LLVMAddFunction *module* name (build-expr type))]
      (native/LLVMSetFunctionCallConv new-fn native/LLVMCCallConv)
      (native/LLVMSetLinkage new-fn native/LLVMExternalLinkage)
      (if body
        (binding [*current-fn* {:expr new-fn :ast ast}]
          (build-expr body)))
      (swap! *fns* assoc name ast)
      (swap! *built-values* assoc id new-fn)
      new-fn)))

(defmethod build-expr :get-fn [{:keys [name]}]
  (native/LLVMGetNamedFunction *module* (str name)))

(defmethod build-expr :get-global [{:keys [name]}]
  (native/LLVMGetNamedGlobal *module* (str name)))

(defmethod build-expr :call [{fn- :fn args :args :as ast}]
  (let [ret-void? (= :void (-> ast return-type :kind))
        name      (if ret-void? "" (str (gensym "invoke")))
        arg-ptr   (native/map-parr build-expr args)]
    (native/LLVMBuildCall *builder*
                          (build-expr fn-)
                          arg-ptr
                          (count args)
                          name)))

(defmethod build-expr :load [{var- :var}]
  (native/LLVMBuildLoad *builder*
                        (build-expr var-)
                        (or (var- :name) (str (gensym "load")))))

(defmethod build-expr :param [{:keys [idx]}]
  (native/LLVMGetParam (*current-fn* :expr) idx))

(defmethod build-expr :ret [{:keys [val]}]
  (if val
    (native/LLVMBuildRet *builder* (build-expr val))
    (native/LLVMBuildRetVoid *builder*)))

(defmethod build-expr :init-global [{:keys [name type id val] :as ast}]
  (if-let [maybe-global (@*built-values* id)]
    maybe-global
    (let [global (native/LLVMAddGlobal *module* (build-expr type) name)]
      (swap! *globals* assoc name ast)
      (if val (native/LLVMSetInitializer global (build-expr val)))
      (swap! *built-values* assoc id global)
      global)))

(defmethod build-expr :store [{:keys [var val]}]
  (native/LLVMBuildStore *builder* (build-expr val) (build-expr var)))

(defmethod build-expr :get-element-ptr [{:keys [pointer idx in-bounds?]}]
  (let [build-fn (if in-bounds?
                   native/LLVMBuildInBoundsGEP
                   native/LLVMBuildGEP)]
    (build-fn *builder*
              (build-expr pointer)
              (native/map-parr build-expr idx)
              (count idx)
              (str (gensym "gep")))))

(defmethod build-expr :type [ast]
  (build-type ast))

(defmethod build-expr :default [{:keys [op] :as ast}]
  (throw+
    {:type ::unknown-expr
     :node ast}
    (str "Don't know how to build expression of type " op)))



(defmethod build-type :fn-type [{:keys [arg-types ret-type variadic?]}]
  (native/LLVMFunctionType (build-expr ret-type)
                           (native/map-parr build-expr arg-types)
                           (count arg-types)
                           variadic?))

(defmethod build-type :struct-type [{:keys [member-types]}]
  (let [struct-type (native/LLVMStructType (native/map-parr build-expr
                                                            member-types)
                                           (count member-types)
                                           false)]
    struct-type))

(defmethod build-type :int [{:keys [width]}]
  (native/LLVMIntType width))

(defmethod build-type :pointer [{:keys [el-type]}]
  (native/LLVMPointerType (build-expr el-type) 0))

(defmethod build-type :array [{:keys [el-type count]}]
  (native/LLVMArrayType (build-expr el-type) count))

(defmethod build-type :void [ast]
  (native/LLVMVoidType))

(defmethod build-type :default [{:keys [kind] :as ast}]
  (throw+
    {:type ::unknown-type
     :node ast}
    (str "Don't know how to build type of kind " kind)))


; TODO: Signed/unsigned
(defmethod build-const :int [{:keys [type val]}]
  (native/LLVMConstInt (build-expr type) val true))

(defmethod build-const :pointer [{:keys [type val] :as ast}]
  (if (nil? val)
    (native/LLVMConstNull (build-expr type))
    (throw+ {:type ::non-nil-const-pointer
             :node ast}
            (str "Can't build non-nil pointer of type " type))))

(defmethod build-const :array [{:keys [type val]}]
  (let [el-type (type :el-type)
        global  (native/LLVMAddGlobal *module*
                                      (build-expr type)
                                      (str (gensym "str")))
        const   (native/LLVMConstArray (build-expr el-type)
                                       (native/map-parr build-expr val)
                                       (count val))]
    (native/LLVMSetInitializer global const)
    global))

(defmethod build-const :default [{:keys [type] :as ast}]
  (throw+
    {:type ::unknown-const
     :node ast}
    (str "Don't know how to build const of type " type)))



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



(defmethod cast-kind* [:int :int] [ast [from-type to-type] kinds]
  (cond
    (< (:width from-type) (:width to-type)) :zext
    (> (:width from-type) (:width to-type)) :trunc
    :else                                   false))

(defmethod cast-kind* [:array :pointer] [ast types kinds]
  :bitcast)

(defmethod cast-kind* [:pointer :pointer] [ast types kinds]
  :bitcast)

(defmethod cast-kind* :default [ast types kinds]
  (throw+
    {:type ::unknown-cast
     :types types
     :kinds kinds}
    (str "Don't know how to build cast from kinds " kinds)))



(defn build-module [{:keys [name exprs]}]
  (let [module (native/LLVMModuleCreateWithName (str name))]
    (binding [*module*       module
              *globals*      (atom {})
              *fns*          (atom {})
              *built-values* (atom {})]
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
  (native/LLVMCreateTargetMachine (-> (native/target-seq) first :target)
                                  (native/LLVMGetDefaultTargetTriple)
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

(defn module-to-ir-string [module]
  (let [ir  (native/LLVMPrintModuleToString module)
        str (.getString ir 0)]
    (native/LLVMDisposeMessage ir)
    str))

(defn optimize [module]
  (let [pass-manager (native/LLVMCreatePassManager)]
    (add-default-passes pass-manager)
    (native/LLVMRunPassManager pass-manager module)
    (native/LLVMDisposePassManager pass-manager))
  module)

(def kw->file-type
  {:assembly-file native/LLVMAssemblyFile
   :object-file   native/LLVMObjectFile})

(defn module-to-file-type [module file-type output-file]
  (let [target        (create-target-machine)
        output-type   (kw->file-type file-type)
        error-message (native/new-pointer)
        error?        (native/LLVMTargetMachineEmitToFile target
                                                          module
                                                          output-file
                                                          output-type
                                                          error-message)]
      (assert (not error?) (.getString (native/value-at error-message) 0)))
  output-file)

(defn module-to-object [module output-file]
  (module-to-file-type module :object-file output-file))

(defn module-to-assembly [module output-file]
  (module-to-file-type module :assembly-file output-file))

(defn objects-to-lib [object-files lib-file]
  (let [command (concat ["gcc" "-dynamiclib" "-o" lib-file] object-files)
        result (apply shell/sh command)]
    (if (not= (result :exit) 0)
      (throw+ {:type ::build-failed
               :result result}
              (str "Compilation failed!\n" (result :err))))))

(defn objects-to-executable [object-files exe-file]
  (let [command (concat ["gcc" "-o" exe-file] object-files)
        result (apply shell/sh command)]
    (if (not= (result :exit) 0)
      (throw+ {:type ::build-failed
               :result result}
              (str "Compilation failed!\n" (result :err)))))
  exe-file)

(defn assembly-to-executable [assembly-file exe-file]
  (let [result (shell/sh "cc" assembly-file "-o" exe-file)]
    (if (not= (result :exit) 0)
      (throw+ {:type ::build-failed
               :result result}
              (str "Compilation failed!\n" (result :err))))))
