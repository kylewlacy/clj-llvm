(ns clj-llvm.compiler-spec
  (:require [speclj.core         :refer :all]
            [clj-llvm.compiler   :refer :all]
            [clj-llvm.runtime    :refer :all]
            [clj-llvm.llvm       :as llvm]
            [clj-llvm.llvm.types :as types]))

(defn compile-and-run
  ([program]
    (compile-and-run program nil))
  ([program lib]
    (let [filename (get-temp-filename)]
      (compile-forms program (if lib [lib] nil) 'user filename)
      (clojure.java.shell/sh filename))))

(describe "The compiler"
  (context "supporting Clojure features"
    (it "can compile 'do' statements"
      (let [program
            '[
              (def -main (fn* -main []
                (. clj-llvm.runtime printf (do
                  (. clj-llvm.runtime printf "Inside a do\n")
                  "Returned from do"))
                0))
            ]

            result
            (compile-and-run program)]
        (should= "Inside a do\nReturned from do" (result :out))))

    (it "can compile 'let' statements"
      (let [program
            '[
              (def -main (fn* -main []
                (let* [fmt "%s bar %s"
                       foo "foo"
                       baz "baz"]
                  (. clj-llvm.runtime printf fmt foo baz))
                0))
            ]

            result
            (compile-and-run program)]
        (should= "foo bar baz" (result :out))))

    (it "can compile 'def' statements"
      (let [program
            '[
              (def foo 3)
              (def bar (fn* -main [] 4))
              (def -main (fn* -main []
                (. clj-llvm.runtime printf "%ld %ld"
                                           foo
                                           (bar))
                0))
            ]

            result
            (compile-and-run program)]
        (should= "3 4" (result :out)))))

  (context "interoping with C"
    (it "can return a status code"
      (let [program
            '[
              (def -main (fn* -main []
                42))
            ]

            result
            (compile-and-run program)]
        (should= 42 (result :exit))))

    (it "can call libc functions"
      (let [program
            '[
              (def -main (fn* -main []
                (. clj-llvm.runtime printf "Hello wrold!")
                0))
            ]

            result
            (compile-and-run program)]
        (should= "Hello wrold!" (result :out))))

    (it "can call custom LLVM functions"
      (let [test-lib
            (lib 'test-lib
              (defn* test-fn [types/Int8* x types/Int64 y -> types/Int64]
                (llvm/ret y)))

            program
            '[
              (def -main (fn* -main []
                (. clj-llvm.runtime printf "%ld"
                                           (. test-lib test-fn "Hello" 4))
                0))
            ]

            result
            (compile-and-run program test-lib)]
          (should= "4" (result :out))))

    (it "can create and use structs"
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
                  (. clj-llvm.runtime printf "%ld %ld %s"
                                             (. struct foo)
                                             (. struct bar)
                                             (. struct baz)))
                0))
            ]

            result
            (compile-and-run program test-lib)]
        (should= "3 4 the-baz" (result :out))))))
