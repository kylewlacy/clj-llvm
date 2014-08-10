(ns cljl.clang.c-source-reader
  (:require [cljl.clang.interop  :as    interop]
            [cljl.native-interop :refer [make-callback]])
  (:import (cljl.clang.interop CXCursor)
           (com.sun.jna Pointer StringArray)))

(defn spelling->string [spelling]
  (let [string (interop/clang_getCString spelling)]
    (interop/clang_disposeString spelling)
    string))

(defn get-cursor-string [cursor]
  (spelling->string (interop/clang_getCursorSpelling cursor)))

(defn get-type-kind [cx-type]
  (.kind cx-type))

(defn size-of [cx-type]
  (interop/clang_Type_getSizeOf cx-type))

(defn width-of [cx-type]
  (* 8 (size-of cx-type)))

(defn walk-cursor [cursor fn-]
  (let [callback (make-callback [CXCursor CXCursor Pointer -> Integer] fn-)]
    (interop/clang_visitChildren cursor callback nil)))

(defn walk-file [file fn-]
  (let [index      (interop/clang_createIndex 0 0)
        args       ["-I/usr/include"]
        trans-unit (interop/clang_parseTranslationUnit
                      index
                      file
                      (into-array String args)
                      (count args)
                      nil
                      0
                      interop/CXTranslationUnit_None)
        cursor     (interop/clang_getTranslationUnitCursor trans-unit)]
    (walk-cursor cursor fn-)
    (interop/clang_disposeTranslationUnit trans-unit)
    (interop/clang_disposeIndex index)))


(defmulti type->map get-type-kind)

(defmethod type->map interop/CXType_FunctionProto [fn-type]
  (let [arg-count  (interop/clang_getNumArgTypes fn-type)
        arg-types  (mapv #(type->map (interop/clang_getArgType fn-type %))
                         (range arg-count))
        ret-type  (type->map (interop/clang_getResultType fn-type))
        variadic? (interop/clang_isFunctionTypeVariadic fn-type)]
    {:clang/op  :type
     :kind      :fn
     :arg-types arg-types
     :ret-type  ret-type
     :variadic? variadic?}))

(defn int-type->map [type signed?]
  {:clang/op :type
   :kind     :int
   :width    (width-of type)
   :size     (size-of type)
   :signed?  signed?})

(defn float-type->map [type]
  {:clang/op :type
   :kind     :float
   :width    (width-of type)
   :size     (size-of type)})

(defmethod type->map interop/CXType_Bool [type]
  (int-type->map type false))

(defmethod type->map interop/CXType_Char_U [type]
  (int-type->map type false))

(defmethod type->map interop/CXType_UChar [type]
  (int-type->map type false))

(defmethod type->map interop/CXType_Char16 [type]
  (int-type->map type true))

(defmethod type->map interop/CXType_Char32 [type]
  (int-type->map type true))

(defmethod type->map interop/CXType_UShort [type]
  (int-type->map type false))

(defmethod type->map interop/CXType_UInt [type]
  (int-type->map type false))

(defmethod type->map interop/CXType_ULong [type]
  (int-type->map type false))

(defmethod type->map interop/CXType_ULongLong [type]
  (int-type->map type false))

(defmethod type->map interop/CXType_UInt128 [type]
  (int-type->map type false))

(defmethod type->map interop/CXType_Char_S [type]
  (int-type->map type true))

(defmethod type->map interop/CXType_SChar [type]
  (int-type->map type true))

(defmethod type->map interop/CXType_WChar [type]
  (int-type->map type true))

(defmethod type->map interop/CXType_Short [type]
  (int-type->map type true))

(defmethod type->map interop/CXType_Int [type]
  (int-type->map type true))

(defmethod type->map interop/CXType_Long [type]
  (int-type->map type true))

(defmethod type->map interop/CXType_LongLong [type]
  (int-type->map type true))

(defmethod type->map interop/CXType_Int128 [type]
  (int-type->map type true))

(defmethod type->map interop/CXType_Float [type]
  (float-type->map type))

(defmethod type->map interop/CXType_Double [type]
  (float-type->map type))

(defmethod type->map interop/CXType_LongDouble [type]
  (float-type->map type))

(defmethod type->map interop/CXType_Pointer [type]
  {:clang/op :type
   :kind     :pointer
   :size     (size-of type)
   :el-type  (type->map (interop/clang_getPointeeType type))})

(defmethod type->map interop/CXType_ConstantArray [type]
  {:clang/op :type
   :kind     :array
   :el-type  (type->map (interop/clang_getElementType type))
   :length   (interop/clang_getNumElements type)
   :size     (size-of type)})

(defmethod type->map interop/CXType_Typedef [type]
  (type->map (interop/clang_getCanonicalType type)))

(defmethod type->map interop/CXType_Void [type]
  {:clang/op :type
   :kind     :void
   :size     0})

(defn get-fields [cursor]
  (let [fields (atom [])]
    (walk-cursor cursor
      (fn [cursor parent _]
        (when (= interop/CXCursor_FieldDecl
                 (interop/clang_getCursorKind cursor))
          (swap! fields
                 conj
                 {:name (get-cursor-string cursor)
                  :type (type->map (interop/clang_getCursorType cursor))}))
        interop/CXChildVisit_Continue))
    @fields))

(defn get-args [cursor]
  (let [args (atom [])]
    (walk-cursor cursor
      (fn [cursor parent _]
        (when (= interop/CXCursor_ParmDecl
                 (interop/clang_getCursorKind cursor))
          (swap! args
                 conj
                 {:name (get-cursor-string cursor)
                  :type (type->map (interop/clang_getCursorType cursor))}))
        interop/CXChildVisit_Continue))
    @args))

(defn cursor->struct-type-map [cursor]
  (let [cursor-type (interop/clang_getCursorType cursor)
        fields      (get-fields cursor)]
    {:clang/op :type
     :kind     :struct
     :fields   fields
     :size     (size-of cursor-type)}))

(defn cursor->union-type-map [cursor]
  (let [cursor-type (interop/clang_getCursorType cursor)
        fields      (get-fields cursor)]
    {:clang/op :type
     :kind     :union
     :fields   fields
     :size     (size-of cursor-type)}))

(defn cursor->function-type-map [cursor]
  (let [cursor-type (interop/clang_getCursorType cursor)
        args        (get-args cursor)]
    {:clang/op  :type
     :kind      :fn
     :args      args
     :arg-types (mapv :type args)
     :ret-type  (type->map (interop/clang_getResultType cursor-type))}))

(defmethod type->map interop/CXType_Record [record-type]
  (cursor->struct-type-map (interop/clang_getTypeDeclaration record-type)))

(defmethod type->map interop/CXType_Unexposed [unexposed-type]
  {:clang/op :type
   :kind     :unknown
   :size     (size-of unexposed-type)})

(defmethod type->map :default [type]
  (println "Don't know how to handle type kind" (get-type-kind type)
           "for type" (spelling->string (interop/clang_getTypeSpelling type)))
  {:clang/op :type
   :kind     :unknown
   :size     (size-of type)})


(defmulti cursor->map interop/clang_getCursorKind)

(defmethod cursor->map interop/CXCursor_VarDecl [cursor]
  {:clang/op :var
   :name     (get-cursor-string cursor)
   :type     (type->map (interop/clang_getCursorType cursor))})

(defmethod cursor->map interop/CXCursor_FunctionDecl [cursor]
  {:clang/op :fn
   :name     (get-cursor-string cursor)
   :type     (cursor->function-type-map cursor)})

(defmethod cursor->map interop/CXCursor_StructDecl [cursor]
  {:clang/op :struct
   :name     (get-cursor-string cursor)
   :type     (cursor->struct-type-map cursor)})

(defmethod cursor->map interop/CXCursor_UnionDecl [cursor]
  {:clang/op :union
   :name     (get-cursor-string cursor)
   :type     (cursor->union-type-map cursor)})

(defmethod cursor->map interop/CXCursor_TypedefDecl [cursor]
  {:clang/op :alias-type
   :name     (get-cursor-string cursor)
   :type     (type->map (interop/clang_getCursorType cursor))})

(defmethod cursor->map :default [cursor]
  (println "Unknown type kind" (spelling->string
                                 (interop/clang_getCursorKindSpelling
                                   (interop/clang_getCursorKind cursor))))
  {:clang/op :unknown})

(defn parse [file]
  (let [*decls* (atom {})]
    (walk-file file
      (fn [cursor parent _]
        (swap! *decls* assoc (get-cursor-string cursor) (cursor->map cursor))
        interop/CXChildVisit_Continue))
    @*decls*))
