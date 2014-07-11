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
  (it "can compile a program that returns a status code"
    (let [program
          '[
            (def -main (fn* -main []
              42))
          ]

          result
          (apply compile-and-run program)]
      (should= 42 (result :exit))))
      
  (it "can compile a program that calls libc functions"
    (let [program
          '[
            (def -main (fn* -main []
              (. clj-llvm.runtime printf "Hello wrold!")
              0))
          ]

          result
          (apply compile-and-run program)]
      (should= "Hello wrold!" (result :out)))))
