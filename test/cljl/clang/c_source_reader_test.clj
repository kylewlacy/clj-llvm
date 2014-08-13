(ns cljl.clang.c-source-reader-test
  (:require [midje.sweet                :refer :all]
            [cljl.clang.c-source-reader :refer :all]
            [cljl.compiler              :refer [get-temp-filename]]))

; http://stackoverflow.com/a/1677927/1311454
(defn map-keys [f hashmap]
  (into {} (for [[k v] hashmap] [k (f v)])))

(defn contains-within [x]
  (cond
    (map? x)                (contains (map-keys contains-within x))
    (or (coll? x) (seq? x)) (contains (map contains-within x))
    :else                   x))

(defn parse-header [header]
  (let [filename (get-temp-filename ".h")]
    (spit filename header)
    (parse filename)))

(facts "About the C source parser"
  (fact "function types are parsed"
      (parse-header "long long foo(int bar, float);")
        => (contains-within
             {"foo" {:type {:kind      :fn
                            :args      [{:name "bar"
                                         :type {:kind :int}}
                                        {:type {:kind :float}}]
                            :arg-types [{:kind :int} {:kind :float}]
                            :ret-type  {:kind :int}}}}))
  (fact "global types are parsed"
    (parse-header "char* fooBar;")
      => (contains-within
           {"fooBar" {:type {:kind    :pointer
                             :el-type {:kind  :int
                                       :width 8}}}}))
  (facts "about parsing primitive types"
    (fact "different integer types are parsed"
      (parse-header "char alpha;
                     short beta;
                     int gamma;
                     long long delta;
                     unsigned char epsilon;
                     unsigned short zeta;
                     unsigned int eta;
                     unsigned long long theta;")
        => (contains-within
             {"alpha"   {:type {:kind    :int
                                :width   8
                                :signed? true}}
              "beta"    {:type {:kind    :int
                                :width   16
                                :signed? true}}
              "gamma"   {:type {:kind    :int
                                :width   32
                                :signed? true}}
              "delta"   {:type {:kind    :int
                                :width   64
                                :signed? true}}
              "epsilon" {:type {:kind    :int
                                :width   8
                                :signed? false}}
              "zeta"    {:type {:kind    :int
                                :width   16
                                :signed? false}}
              "eta"     {:type {:kind    :int
                                :width   32
                                :signed? false}}
              "theta"   {:type {:kind    :int
                                :width   64
                                :signed? false}}}))
    (fact "different float types are parsed"
      (parse-header "float alpha; double beta;")
        => (contains-within
             {"alpha" {:type {:kind :float, :width 32}}
              "beta"  {:type {:kind :float, :width 64}}}))
    (fact "pointer types are parsed"
      (parse-header "int* alpha; void* beta; char* gamma;")
        => (contains-within
             {"alpha" {:type {:kind    :pointer
                              :el-type {:kind :int}}}
              "beta"  {:type {:kind    :pointer
                              :el-type {:kind :void}}}
              "gamma" {:type {:kind    :pointer
                              :el-type {:kind :int}}}}))
    (fact "array types are parsed"
      (parse-header "int foo[16];")
        => (contains-within
             {"foo" {:type {:kind    :array
                            :length  16
                            :size    64
                            :el-type {:kind :int}}}}))
    (fact "struct types are parsed"
      (let [parsed   (parse-header "struct FooStruct {
                                      int foo;
                                      float bar;
                                      char* baz;
                                    };
                                    void* quux;")
            ptr-size (get-in parsed ["quux" :type :size])]
        parsed => (contains-within
                    {"FooStruct" {:type {:kind   :struct
                                         :size   (+ 4 4 ptr-size)
                                         :fields [
                                           {:name "foo"
                                            :type {:kind :int}}
                                           {:name "bar"
                                            :type {:kind :float}}
                                           {:name "baz"
                                            :type {:kind :pointer}}]}}})))
    (fact "union types are parsed"
      (parse-header "union FooUnion {
                       int   fooInt;
                       float fooFloat;
                       void* fooData;
                       int   fooArr[16];
                     }")
        => (contains-within
             {"FooUnion" {:type {:kind   :union
                                 :size   64
                                 :fields [
                                   {:name "fooInt"
                                    :type {:kind :int}}
                                   {:name "fooFloat"
                                    :type {:kind :float}}
                                   {:name "fooData"
                                    :type {:kind :pointer}}
                                   {:name "fooArr"
                                    :type {:kind :array}}]}}}))
    (fact "typedefs are parsed"
      (parse-header "typedef unsigned long long my_time_t")
        => (contains-within
            {"my_time_t" {:type {:kind    :int
                                 :width   64
                                 :signed? false}}}))))
