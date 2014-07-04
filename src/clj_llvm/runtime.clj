(ns clj-llvm.runtime
  (:require [mjolnir.types       :refer :all]
            [mjolnir.expressions :as    expr]
            [mjolnir.constructors-init])
  (:alias c mjolnir.constructors))

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
    `(let [f# (c/fn ~(str name)
                    (c/fn-t ~(mapv first args)
                            ~ret-type
                            ~variadic?)
                    ~(mapv second args)
                    :extern
                    ~@body)]
        (swap! *globals* assoc '~name f#)
        f#)))

(defmacro deflib [name & body]
  `(binding [*globals* (atom {})]
    (let [exprs# (vector ~@body)]
      (def ~name {:exprs exprs# :globals @*globals*}))))

(deflib runtime-lib
  (c-defn rand [-> Int32])
  (c-defn srand [Int32 seed -> VoidT])
  (c-defn time [Int64* timer -> Int64])

  (c-defn inc [Int64 x -> Int64]
    (c/+ x (c/const 1 -> Int64)))

  (c-defn my-rand [-> Int64]
    (c/cast Int64 (expr/->Call (expr/->Gbl "rand")
                               [])))

  (c-defn my-srand [Int64 seed -> Int64]
    (expr/->Call (expr/->Gbl "srand")
                 [(c/cast Int32 seed)])
    seed)

  (c-defn printf [Int8* format & more -> Int32])

  (c-defn my-time [-> Int64]
    (expr/->Call (expr/->Gbl "time")
                 [(c/const 0 -> Int64*)])))
