(ns clj-llvm.java-interop
  (:import (clojure.asm         Opcodes Type ClassWriter)
           (clojure.asm.commons Method GeneratorAdapter)))

(def ^:dynamic *version* Opcodes/V1_6)

(def modifier->opcode
  {:abstract  Opcodes/ACC_ABSTRACT
   :enum      Opcodes/ACC_ENUM
   :final     Opcodes/ACC_FINAL
   :interface Opcodes/ACC_INTERFACE
   :private   Opcodes/ACC_PRIVATE
   :protected Opcodes/ACC_PROTECTED
   :public    Opcodes/ACC_PUBLIC
   :static    Opcodes/ACC_STATIC})

(defn modifiers->opcode [modifiers]
  (reduce bit-or (map modifier->opcode modifiers)))

(defn internal-name [type]
  (.getInternalName (Type/getType type)))

(defn method-desc [arg-types ret-type]
  (Type/getMethodDescriptor (Type/getType ret-type)
                            (into-array Type
                                        (map #(Type/getType %) arg-types))))



(defn create-class [name modifiers superclass interfaces]
  (let [class-writer (ClassWriter. ClassWriter/COMPUTE_FRAMES)]
    (.visit class-writer
            *version*
            (modifiers->opcode modifiers)
            (str name)
            nil
            (internal-name superclass)
            (into-array String (map internal-name interfaces)))
    class-writer))



(defn add-field [class name modifiers type]
  (->
    class
    (.visitField (modifiers->opcode modifiers)
                 (str name)
                 (Type/getDescriptor type)
                 nil
                 nil)
    (.visitEnd)))



(defn load-class [class name]
  (.visitEnd class)
  (let [bytes        (.toByteArray class)
        class-loader (clojure.lang.DynamicClassLoader.)]
    (.defineClass class-loader (str name) bytes nil)))



(defn create-method [name arg-types ret-type]
  (Method. name (method-desc arg-types ret-type)))

(defn create-method-adapter [class modifiers method]
  (GeneratorAdapter. (modifiers->opcode modifiers)
                                   method
                                   nil
                                   nil
                                   class))

(defn init-method [class name modifiers arg-types ret-type]
  (create-method-adapter class
                         modifiers
                         (create-method name arg-types ret-type)))



(defn create-ctor [arg-types]
  (create-method "<init>" arg-types Void/TYPE))

(defn init-ctor [class modifiers arg-types]
  (create-method-adapter class modifiers (create-ctor arg-types)))

(defn add-superclass-ctor [class superclass modifiers arg-types]
  (let [method  (create-ctor arg-types)
        ctor    (create-method-adapter class modifiers method)]

    (.visitCode ctor)
    (.loadThis ctor)
    (doseq [idx (range (count arg-types))]
      (.loadArg ctor idx))
    (doto ctor
      (.invokeConstructor (Type/getType superclass)
                          method)
      (.returnValue)
      (.endMethod))))

(defn make-class [name modifiers superclass-name interface-names & members]
  (let [superclass (resolve superclass-name)
        interfaces (map resolve interface-names)
        class      (create-class name modifiers superclass interfaces)]
    (doseq [member members]
      (cond
        (symbol? member)
          (add-field class member [:public] (resolve ((meta member) :tag)))
        (list? member)
          (if (= superclass-name (first member))
            (add-superclass-ctor class
                                 superclass
                                 [:public]
                                 (map resolve (second member)))
            (throw (ex-info (str "Don't know how to make member from " member)
                            {:type   ::unknown-member
                             :member member
                             :class  name})))))
    (load-class class name)))

(defn def-class* [name & opts+members]
  (let [pairs                    (partition-all 2 opts+members)
        pair-is-opt?             #(keyword? (first %))
        [opt-pairs member-pairs] (split-with pair-is-opt? pairs)
        opts                     (apply hash-map (mapcat identity opt-pairs))
        members                  (mapcat identity member-pairs)
        superclass-name          (or (opts :extends) 'Object)
        interface-names          (or (opts :implements) [])]
    (apply make-class name [:public] superclass-name interface-names members)))

(defmacro def-class [name & opts+members]
  `(do
    (apply def-class* '~name '~opts+members)
    (import '~(symbol (str (ns-name *ns*)) (str name)))))
