(ns cljl.llvm
  (:require [cljl.llvm.types :as types]))

(defmulti declaration-for :llvm/op)



(defn alloca
  ([type] (alloca type (str (gensym "local"))))
  ([type name]
    {:llvm/op :alloca
     :id      (gensym "alloca")
     :type    type
     :name    name}))

(defn block [& statements]
  {:llvm/op    :block
   :statements statements})

(defn call [fn- & args]
  {:llvm/op :call
   :fn      fn-
   :args    args})

(defn cast- [expr to-type]
  {:llvm/op :cast
   :expr    expr
   :to-type to-type})

(defn const [type val]
  {:llvm/op :const
   :type    type
   :val     val})

(defn doall- [& statements]
  {:llvm/op    :doall
   :statements (butlast statements)
   :ret        (last statements)})

(defn fn-
  ([name type linkage]
    (fn- name type linkage nil))
  ([name type linkage body]
    (let [void? (= :void (-> type :ret-type :kind))]
      {:llvm/op :fn
       :id      (gensym "fn")
       :name    name
       :type    type
       :linkage linkage
       :body    body})))

(defn get-element-ptr [pointer & idx]
  {:llvm/op :get-element-ptr
   :pointer pointer
   :idx     idx})

(defn get-element-ptr-in-bounds [pointer & idx]
  (assoc (apply get-element-ptr pointer idx) :in-bounds? true))

(defn get-global [name]
  {:llvm/op :get-global
   :name    name})

(defn get-fn [name]
  {:llvm/op :get-fn
   :name    name})

(defn init-global
  ([name type]
    (init-global name type nil))
  ([name type val]
    {:llvm/op :init-global
     :id      (gensym "global")
     :name    name
     :type    type
     :val     val}))

(defn load- [var]
  {:llvm/op :load
   :var     var})

(defn module [name & exprs]
  {:llvm/op :module
   :name    name
   :exprs   exprs})

(defn param [idx]
  {:llvm/op :param
   :idx     idx})

(defn ret [val]
  {:llvm/op :ret
   :val     val})

(defn store [var val]
  {:llvm/op :store
   :var     var
   :val     val})



(defmethod declaration-for :fn [fn-]
  (assoc fn- :body nil))

(defmethod declaration-for :init-global [global]
  (assoc global :val nil))
