(ns clj-llvm.compiler-spec
  (:require [speclj.core :refer :all]
            [clj-llvm.compiler :refer :all]))

(defn get-temp-filename []
  (let [temp-file
          (java.io.File/createTempFile (str (gensym "temp"))
                                       nil
                                       nil)]
    (.deleteOnExit temp-file)
    (.toString temp-file)))

(defn compile-and-run [& forms]
  (let [filename (get-temp-filename)]
    (compile-forms forms 'user filename)
    (clojure.java.shell/sh filename)))

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
            (apply compile-and-run program)]
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
            (apply compile-and-run program)]
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
            (apply compile-and-run program)]
        (should= "3 4" (result :out)))))
  (context "interoping with C"
    (it "can return a status code"
      (let [program
            '[
              (def -main (fn* -main []
                42))
            ]

            result
            (apply compile-and-run program)]
        (should= 42 (result :exit))))

    (it "can call libc functions"
      (let [program
            '[
              (def -main (fn* -main []
                (. clj-llvm.runtime printf "Hello wrold!")
                0))
            ]

            result
            (apply compile-and-run program)]
        (should= "Hello wrold!" (result :out))))))
