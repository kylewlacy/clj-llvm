(ns clj-llvm.llvm
  (:require [clj-llvm.llvm.native :as native]
            [clj-llvm.llvm.types  :as types]))



(defn do- [statements ret]
  {:op :do
   :statements statements
   :ret ret})

(defn cast- [expr to-type]
  {:op :cast
   :expr expr
   :to-type to-type})

(defn const [type val]
  {:op :const
   :type type
   :val val})

(defn invoke [fn- & args]
  {:op :invoke
   :fn fn-
   :args args})

(defn param [idx]
  {:op :param
   :idx idx})

(defn get-global [name]
  {:op :get-global
   :name name})

(defn get-fn [name]
  {:op :get-fn
   :name name})

(defn set-global [name type init]
  {:op :set-global
   :name name
   :type type
   :init init})

(defn fn-
  ([name type linkage]
    (fn- name type linkage nil))
  ([name type linkage body]
    (let [void? (= :void (-> type :ret-type :kind))]
      {:op :fn
       :name name
       :type type
       :linkage linkage
       :body body})))

(defn module [name & exprs]
  {:op :module
   :name name
   :exprs exprs})
