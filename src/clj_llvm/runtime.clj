(ns clj-llvm.runtime
  (:require [clj-llvm.llvm       :as llvm]
            [clj-llvm.llvm.types :as types]))

(def ^:dynamic *globals*)

(defn dbg [& args] (apply println args) (last args))

; Identical to c/defn, but doesn't actually define a callable fn
(defmacro c-defn [name args & body]
  {:pre [(even? (count args))]}
  (let [args          (partition 2 args)
        variadic-fn   (comp (partial = '&) first)
        variadic-name (second (first (filter variadic-fn args)))
        ret-fn        (comp (partial = '->) first)
        ret-type      (second (first (filter ret-fn args)))
        args          (remove variadic-fn (remove ret-fn args))
        args-map      (zipmap (map second args)
                              (range))
        arg-types     (mapv first args)
        variadic?     (not (nil? variadic-name))]
    (assert ret-type
            (str "Compiling: " name "No return type given, did "
                                    "you forget the -> type?"))

    `(let ~(vec (mapcat #(vector %1 (llvm/param %2))
                        (map second args)
                        (range)))
      (let [fn-type# (types/FnType ~(mapv first args)
                                   ~ret-type
                                   ~variadic?)
            void?# (= :void (~ret-type :kind))
            body# (vector ~@body)
            f# (llvm/fn- ~(str name)
                         (types/FnType ~(mapv first args)
                                       ~ret-type
                                       ~variadic?)
                         :extern
                         (when-not (empty? body#)
                           (llvm/do- (if void?# body#
                                                (butlast body#))
                                     (if void?# nil
                                                (last body#)))))]
          (swap! *globals* assoc '~name f#)
          f#))))

(defmacro deflib [name & body]
  `(binding [*globals* (atom {})]
    (let [exprs# (vector ~@body)]
      (def ~name {:exprs exprs# :globals @*globals*}))))

(deflib runtime-lib
  (c-defn rand [-> types/Int32])
  (c-defn srand [types/Int32 seed -> types/VoidT])
  (c-defn time [types/Int64* timer -> types/Int64])

  (c-defn my-rand [-> types/Int64]
    (llvm/cast- (llvm/invoke (llvm/get-fn "rand")
                             [])
                types/Int64))

  (c-defn my-srand [types/Int64 seed -> types/VoidT]
    (llvm/invoke (llvm/get-fn "srand")
                 [(llvm/cast- seed types/Int32)]))

  (c-defn printf [types/Int8* format & more -> types/Int32])

  (c-defn my-time [-> types/Int64]
    (llvm/invoke (llvm/get-fn "time")
                             [(llvm/const types/Int64* nil)])))
