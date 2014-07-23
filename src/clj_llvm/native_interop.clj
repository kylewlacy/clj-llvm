; Taken from https://github.com/halgari/mjolnir/blob/5b1c0cf1c34d5521438ee33974715d98abb7884d/src/mjolnir/llvmc.clj
(ns clj-llvm.native-interop
  (:import (com.sun.jna Pointer Memory)))

(def ^:dynamic *lib*)

(defmacro with-lib [lib & body]
  `(binding [*lib* '~lib]
    ~@body))

(defn get-function [s]
  (com.sun.jna.Function/getFunction (name *lib*) (name s)))

; (defn dbg [& args] (apply println args) (last args))
(defn native* [return-type function-symbol]
  (let [func (get-function function-symbol)]
    (fn [& args]
      ; (apply println (str function-symbol) "args:" args)
      ; (dbg "  =>" (.invoke func return-type (to-array args))))))
      (.invoke func return-type (to-array args)))))

(defmacro native [return-type function-name]
  `(native* ~return-type '~function-name))

(defmacro defnative [return-type function-name]
  `(def ~function-name (native* ~return-type '~function-name)))

(defmacro defenum
  ([nm defs]
    `(defenum ~nm 0 ~defs))
  ([nm init defs]
    (list* 'do
      `(def ~nm {:idx ~(zipmap (range)
                               (map (comp keyword name) defs))
                 :defs ~(zipmap (map (comp keyword name) defs)
                                (range init Integer/MAX_VALUE))})
      (map (fn [d idx] `(def ~d ~idx))
           defs
           (range init Integer/MAX_VALUE)))))

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

(defn map-parr [fn coll]
  (into-array Pointer
              (map fn coll)))

(defn value-at [ptr]
  (.getPointer ptr 0))
