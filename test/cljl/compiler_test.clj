(ns cljl.compiler-test
  (:require [midje.sweet     :refer :all]
            [cljl.compiler   :refer :all]
            [cljl.runtime    :refer :all]
            [cljl.llvm       :as llvm]
            [cljl.llvm.types :as types]))

(defn compile-and-run
  ([program]
    (compile-and-run program nil))
  ([program lib]
    (let [filename (get-temp-filename)]
      (compile-forms program (if lib [lib] nil) 'user filename)
      (clojure.java.shell/sh filename))))

(facts "About the compiler"
  (facts "about Clojure features"
    (fact "`do` statements return the result of the last expression"
      (let [program
            '[
              (def -main (fn* -main []
                (. cljl.runtime printf (do
                  (. cljl.runtime printf "Inside a do\n")
                  "Returned from do"))
                0))
            ]

            result
            (compile-and-run program)]
        (result :out) => "Inside a do\nReturned from do"))

    (fact "`let*` binds values in the lexical scope"
      (let [program
            '[
              (def -main (fn* -main []
                (let* [fmt "%s bar %s"
                       foo "foo"
                       baz "baz"]
                  (. cljl.runtime printf fmt foo baz))
                0))
            ]

            result
            (compile-and-run program)]
        (result :out) => "foo bar baz"))

    (fact "`def` statements define variables in the global scope"
      (let [program
            '[
              (def foo 3)
              (def bar (fn* -main [] 4))
              (def -main (fn* -main []
                (. cljl.runtime printf "%ld %ld"
                                           foo
                                           (bar))
                0))
            ]

            result
            (compile-and-run program)]
        (result :out) => "3 4"))

  (facts "about C interop"
    (fact "programs can return a status code"
      (let [program
            '[
              (def -main (fn* -main []
                42))
            ]

            result
            (compile-and-run program)]
        (result :exit) => 42))

    (fact "the program can call libc functions"
      (let [program
            '[
              (def -main (fn* -main []
                (. cljl.runtime printf "Hello wrold!")
                0))
            ]

            result
            (compile-and-run program)]
        (result :out) => "Hello wrold!"))

    (fact "the program can call custom LLVM functions"
      (let [test-lib
            (lib 'test-lib
              (defn* test-fn [types/Int8* x types/Int64 y -> types/Int64]
                (llvm/ret y)))

            program
            '[
              (def -main (fn* -main []
                (. cljl.runtime printf "%ld"
                                           (. test-lib test-fn "Hello" 4))
                0))
            ]

            result
            (compile-and-run program test-lib)]
          (result :out) => "4"))

    (fact "the program can create and use structs"
      (let [test-lib
            (lib 'test-lib
              (defstruct* TestStruct
                types/Int64 foo
                types/Int64 bar
                types/Int8* baz))

            program
            '[
              (def -main (fn* -main []
                (let* [struct (. test-lib TestStruct 3 4 "the-baz")]
                  (. cljl.runtime printf "%ld %ld %s"
                                             (. struct foo)
                                             (. struct bar)
                                             (. struct baz)))
                0))
            ]

            result
            (compile-and-run program test-lib)]
        (result :out) => "3 4 the-baz")))))
