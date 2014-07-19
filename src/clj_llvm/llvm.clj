(ns clj-llvm.llvm
  (:require [clj-llvm.llvm.native :as native]
            [clj-llvm.llvm.types  :as types]))



(defn alloca
  ([type] (alloca type (str (gensym "local"))))
  ([type name]
    {:op :alloca
     :id (gensym "alloca")
     :type type
     :name name}))

(defn block [& statements]
  {:op :block
   :statements statements})

(defn call [fn- & args]
  {:op :call
   :fn fn-
   :args args})

(defn cast- [expr to-type]
  {:op :cast
   :expr expr
   :to-type to-type})

(defn const [type val]
  {:op :const
   :type type
   :val val})

(defn doall- [& statements]
  {:op :doall
   :statements (butlast statements)
   :ret (last statements)})

(defn fn-
  ([name type linkage]
    (fn- name type linkage nil))
  ([name type linkage body]
    (let [void? (= :void (-> type :ret-type :kind))]
      {:op :fn
       :id (gensym "fn")
       :name name
       :type type
       :linkage linkage
       :body body})))

(defn get-global [name]
  {:op :get-global
   :name name})

(defn get-fn [name]
  {:op :get-fn
   :name name})

(defn init-global
  ([name type]
    (init-global name type nil))
  ([name type val]
    {:op :init-global
     :id (gensym "global")
     :name name
     :type type
     :val val}))

(defn load- [var]
  {:op :load
   :var var})

(defn module [name & exprs]
  {:op :module
   :name name
   :exprs exprs})

(defn param [idx]
  {:op :param
   :idx idx})

(defn ret [val]
  {:op :ret
   :val val})

(defn store [var val]
  {:op :store
   :var var
   :val val})
