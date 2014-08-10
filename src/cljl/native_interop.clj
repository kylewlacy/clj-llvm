; Taken from https://github.com/halgari/mjolnir/blob/5b1c0cf1c34d5521438ee33974715d98abb7884d/src/mjolnir/llvmc.clj
(ns cljl.native-interop
  (:require [cljl.java-interop :refer [def-class]])
  (:import (com.sun.jna Pointer Memory Callback Structure)))

(def ^:dynamic *lib*)

(defmacro with-lib [lib & body]
  `(binding [*lib* '~lib]
    ~@body))

(defn get-function [s]
  (com.sun.jna.Function/getFunction (name *lib*) (name s)))

#_(defn dbg [& args] (apply println args) (last args))
(defn native* [return-type function-symbol]
  (let [func (get-function function-symbol)]
    (fn [& args]
      #_(apply println (str function-symbol) "args:" args)
      #_(dbg "  =>" (.invoke func return-type (to-array args)))
      (.invoke func return-type (to-array args)))))

(defmacro native [return-type function-name]
  `(native* ~return-type '~function-name))

(defmacro def-native-fn [return-type function-name]
  `(def ~function-name (native* ~return-type '~function-name)))

(defmacro def-enum
  ([nm defs]
    `(def-enum ~nm 0 ~defs))
  ([nm init defs]
    (list* 'do
      `(def ~nm {:idx ~(zipmap (range)
                               (map (comp keyword name) defs))
                 :defs ~(zipmap (map (comp keyword name) defs)
                                (range init Integer/MAX_VALUE))})
      (map (fn [d idx] `(def ~d ~idx))
           defs
           (range init Integer/MAX_VALUE)))))

(defmacro def-native-struct [name & members]
  (let [member-parts (partition 2 members)
        member-types (map first member-parts)
        member-names (map second member-parts)
        typed-fields (mapv #(with-meta %1 {:tag %2})
                           member-names member-types)]
    `(def-class ~name :extends    com.sun.jna.Structure
                      :implements [com.sun.jna.Structure$ByValue]
        (com.sun.jna.Structure [])
        (com.sun.jna.Structure [com.sun.jna.TypeMapper])
        (com.sun.jna.Structure [Integer])
        (com.sun.jna.Structure [Integer com.sun.jna.TypeMapper])
        (com.sun.jna.Structure [com.sun.jna.Pointer])
        (com.sun.jna.Structure [com.sun.jna.Pointer Integer])
        (com.sun.jna.Structure [com.sun.jna.Pointer
                                Integer
                                com.sun.jna.TypeMapper])
        ~@typed-fields)))

(defn new-pointer []
  (let [p (Memory. Pointer/SIZE)]
    (.clear p)
    p))


(defn to-pointers [& args]
  (let [arr (make-array Pointer (count args))]
    (loop [a args
           c 0]
      (if a
        (do (aset arr c (first a))
            (recur (next a) (inc c)))
        arr))))

(defn map-parr [fn- coll]
  (into-array Pointer
              (map fn- coll)))

(defn value-at [ptr]
  (.getPointer ptr 0))

(defmacro make-callback [signature fn-]
  {:pre [(<= 2 (count signature))
         (= '-> (last (butlast signature)))]}
  (let [arg-type-symbols (drop-last 2 signature)
        ret-type-symbol  (last signature)
        arg-types        (map resolve arg-type-symbols)
        ret-type         (resolve ret-type-symbol)
        arg-names        (map #(gensym (str "arg_" %)) arg-type-symbols)
        tagged-method    (with-meta 'callback {:tag ret-type})
        tagged-args      (map #(with-meta %1 {:tag %2}) arg-names arg-types)
        all-args         (conj tagged-args 'this)
        protocol-name    (symbol (str (namespace-munge *ns*) "."
                                      (gensym "CallbackInterface")))
        protocol-method  (vector 'callback (vec arg-types) ret-type)]
    (assert (every? identity arg-types)
            (str "Unknown types in arg list "
                 (vec (remove nil?
                              (map #(if-not %1 %2)
                                   arg-types
                                   arg-type-symbols)))))
    (assert ret-type (str "Unknown return type " ret-type-symbol))
    `(do
      (gen-interface :name    ~protocol-name
                     :methods [~protocol-method]
                     :extends [Callback])
      (import ~protocol-name)
      (reify ~protocol-name
        (~tagged-method ~(vec all-args)
          ~(apply list fn- arg-names))))))
