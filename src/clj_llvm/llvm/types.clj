(ns clj-llvm.llvm.types)

(defn Int [width]
  {:op :type
   :kind :int
   :width width})

(defn Pointer [el-type]
  {:op :type
   :kind :pointer
   :el-type el-type})

(defn Array [el-type count]
  {:op :type
   :kind :array
   :el-type el-type
   :count count})

(def VoidT
  {:op :type
   :kind :void})

(defn FnType
  ([arg-types ret-type]
    (FnType arg-types ret-type false))
  ([arg-types ret-type variadic?]
    {:op :type
     :kind :fn-type
     :arg-types arg-types
     :ret-type ret-type
     :variadic? variadic?}))

(defn StructType
  ([member-types]
    (StructType member-types nil))
  ([member-types member-names]
    {:op :type
     :kind :struct-type
     :member-types member-types
     :member-names member-names
     :idx (apply hash-map (mapcat vector member-names (range)))}))



(def Int8 (Int 8))
(def Int16 (Int 16))
(def Int32 (Int 32))
(def Int64 (Int 64))
(def Int8* (Pointer Int8))
(def Int16* (Pointer Int16))
(def Int32* (Pointer Int32))
(def Int64* (Pointer Int64))
