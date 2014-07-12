(ns clj-llvm.runtime
  (:require [clj-llvm.llvm       :refer :all]
            [clj-llvm.llvm.types :refer :all]))

(def ^:dynamic *globals*)



(defmacro defn* [name args & body]
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

    `(let ~(vec (mapcat #(vector %1 (param %2))
                        (map second args)
                        (range)))
      (let [fn-type# (FnType ~(mapv first args)
                             ~ret-type
                             ~variadic?)
            void?# (= :void (~ret-type :kind))
            body# (vector ~@body)
            body# (if void?# (concat body# [(ret nil)]) body#)
            f# (fn- ~(str name)
                    (FnType ~(mapv first args)
                            ~ret-type
                            ~variadic?)
                    :extern
                    (if-not (empty? body#) (apply block body#)))]
          (swap! *globals* assoc '~name f#)
          f#))))

(defmacro deflib [name & body]
  `(binding [*globals* (atom {})]
    (let [exprs# (vector ~@body)]
      (def ~name {:exprs exprs# :globals @*globals*}))))

(deflib runtime-lib
  (defn* rand [-> Int32])
  (defn* srand [Int32 seed -> VoidT])
  (defn* time [Int64* timer -> Int64])

  (defn* my-rand [-> Int64]
    (ret (cast- (invoke (get-fn "rand")) Int64)))

  (defn* my-srand [Int64 seed -> VoidT]
    (invoke (get-fn "srand")
            (cast- seed Int32)))

  (defn* printf [Int8* format & more -> Int32])

  (defn* my-time [-> Int64]
    (ret (invoke (get-fn "time")
                 (const Int64* nil)))))
