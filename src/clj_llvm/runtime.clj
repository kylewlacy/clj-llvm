(ns clj-llvm.runtime
  (:require [clj-llvm.llvm       :refer :all]
            [clj-llvm.llvm.types :refer :all]))

(def ^:dynamic *globals*)
(def ^:dynamic *decls*)



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
            fn#   (fn- ~(str name)
                       (FnType ~(mapv first args)
                               ~ret-type
                               ~variadic?)
                    :extern
                    (if-not (empty? body#) (apply block body#)))
            decl# (declaration-for fn#)]
          (swap! *globals* assoc '~name fn#)
          (swap! *decls* conj decl#)
          fn#))))

(defmacro defstruct* [name & members]
  (let [members (partition 2 members)
        member-types (mapv first members)
        member-names (mapv second members)]
    `(let [struct# (StructType ~member-types '~member-names)]
      (swap! *globals* assoc '~name struct#)
      struct#)))

(defmacro lib [symbol & body]
  `(binding [*globals* (atom {})
             *decls*   (atom [])]
    (let [exprs# (vector ~@body)]
      {:name ~symbol :exprs exprs# :decls @*decls* :globals @*globals*})))

(defmacro deflib [name symbol & body]
  `(def ~name (lib ~symbol ~@body)))



(deflib runtime-lib 'clj-llvm.runtime
  (defn* rand [-> Int32])
  (defn* srand [Int32 seed -> VoidT])
  (defn* time [Int64* timer -> Int64])

  (defn* my-rand [-> Int64]
    (ret (cast- (call (get-fn "rand")) Int64)))

  (defn* my-srand [Int64 seed -> VoidT]
    (call (get-fn "srand")
          (cast- seed Int32)))

  (defn* printf [Int8* format & more -> Int32])

  (defn* my-time [-> Int64]
    (ret (call (get-fn "time")
               (const Int64* nil)))))
