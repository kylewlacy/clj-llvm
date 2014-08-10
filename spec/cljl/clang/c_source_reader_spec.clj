(ns cljl.clang.c-source-reader-spec
  (:require [speclj.core                :refer :all]
            [cljl.clang.c-source-reader :refer :all]
            [cljl.compiler              :refer [get-temp-filename]]))

; HACK: This doesn't have proper messages or anything
(defn should-match [expected actual]
  (doseq [key (keys expected)]
    (should-contain key actual)
    (let [expected-val (get expected key)
          actual-val   (get actual key)]
      (cond
        (and (map? expected-val) (map? actual-val))
          (should-match expected-val actual-val)
        (and (coll? expected-val) (coll? actual-val))
          (do
            (should= (count expected-val) (count actual-val))
            (doseq [[expected-item actual-item]
                    (map list expected-val actual-val)]
              (should-match expected-item actual-item)))
        :else
          (should= expected-val actual-val)))))

(defn parse-header [header]
  (let [filename (get-temp-filename ".h")]
    (spit filename header)
    (parse filename)))

(describe "The C source analyzer"
  (it "gets function types"
    (let [parsed (parse-header "long long foo(int bar, float);")]
      (should-match {"foo" {:type {:kind      :fn
                                   :args      [{:name "bar"
                                                :type {:kind :int}}
                                               {:type {:kind :float}}]
                                   :arg-types [{:kind :int} {:kind :float}]
                                   :ret-type  {:kind :int}}}}
                    parsed)))
  (it "gets global types"
    (let [parsed (parse-header "char* fooBar;")]
      (should-match {"fooBar" {:type {:kind    :pointer
                                      :el-type {:kind  :int
                                                :width 8}}}}
                    parsed)))
  (context "when parsing types"
    (it "parses integer types"
      (let [parsed (parse-header "char alpha;
                                  short beta;
                                  int gamma;
                                  long long delta;
                                  unsigned char epsilon;
                                  unsigned short zeta;
                                  unsigned int eta;
                                  unsigned long long theta;")]
        (should-match {"alpha"   {:type {:kind    :int
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
                                         :signed? false}}}
                      parsed)))
    (it "parses float types"
      (let [parsed (parse-header "float alpha; double beta;")]
        (should-match {"alpha" {:type {:kind :float, :width 32}}
                       "beta"  {:type {:kind :float, :width 64}}}
                      parsed)))
    (it "parses pointer types"
      (let [parsed (parse-header "int* alpha; void* beta; char* gamma;")]
        (should-match {"alpha" {:type {:kind    :pointer
                                       :el-type {:kind :int}}}
                       "beta"  {:type {:kind    :pointer
                                       :el-type {:kind :void}}}
                       "gamma" {:type {:kind    :pointer
                                       :el-type {:kind :int}}}}
                      parsed)))
    (it "parses array types"
      (let [parsed (parse-header "int foo[16];")]
        (should-match {"foo" {:type {:kind    :array
                                     :el-type {:kind :int}
                                     :length  16
                                     :size    64}}}
                      parsed)))
    (it "parses struct types"
      (let [parsed   (parse-header "struct FooStruct {
                                      int foo;
                                      float bar;
                                      char* baz;
                                    };
                                    void* quux;")
            ptr-size (get-in parsed ["quux" :type :size])]
        (should-match {"FooStruct" {:type {:kind   :struct
                                           :size   (+ 4 4 ptr-size)
                                           :fields [
                                             {:name "foo"
                                              :type {:kind :int}}
                                             {:name "bar"
                                              :type {:kind :float}}
                                             {:name "baz"
                                              :type {:kind :pointer}}]}}}
                      parsed)))
    (it "parses union types"
      (let [parsed (parse-header "union FooUnion {
                                    int   fooInt;
                                    float fooFloat;
                                    void* fooData;
                                    int   fooArr[16];
                                  }")]
        (should-match {"FooUnion" {:type {:kind   :union
                                          :size   64
                                          :fields [
                                            {:name "fooInt"
                                             :type {:kind :int}}
                                            {:name "fooFloat"
                                             :type {:kind :float}}
                                            {:name "fooData"
                                             :type {:kind :pointer}}
                                            {:name "fooArr"
                                             :type {:kind :array}}]}}}
                      parsed)))
    (it "parses typedefs"
      (let [parsed (parse-header "typedef unsigned long long my_time_t")]
        (should-match {"my_time_t" {:type {:kind    :int
                                           :width   64
                                           :signed? false}}}
                      parsed)))))
