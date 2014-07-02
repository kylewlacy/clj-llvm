(ns clj-llvm.analyzer
  (:require [clojure.tools.analyzer       :as    analyzer]
            [clojure.tools.analyzer.utils :as    utils]
            [clojure.tools.macro          :refer [macrolet]]
            [clojure.tools.analyzer.env   :as    env]
            [clojure.java.io              :as    io]))



(def ^:dynamic default-ns 'user)

(defn new-ns [name] {:mappings {}
                     :aliases  {}
                     :ns       name})

(defn empty-local-env [] {:context    :exec
                          :locals     {}
                          :ns         default-ns})

(defn empty-env []
  {:namespaces {default-ns (new-ns default-ns)}
   :ns default-ns})



(def specials (into analyzer/specials '#{deftype*
                                         defrecord*
                                         c-ffi*}))



(defn create-var [name env]
  {:op   :var
   :name name
   :ns   (@env/*env* :ns)})

(defn create-macro-var [name env macro-form]
  (assoc (create-var name env) :macro? true :macro-form macro-form))

(defn var?* [obj]
  (= :var (:op obj)))

(defn get-var [name env]
  (if-let [var* (utils/resolve-var name env)]
    (get-in @env/*env* [:namespaces (var* :ns) :mappings (var* :name)])))



(defn desugar-host-expr [form] form)

; TODO: Fn with multiple bodies
(defn maybe-macroexpand-var [{macro-form :macro-form} form env]
  (if (= 'fn* (first macro-form))
    (let [args       (concat (list form env) (rest form))
          macro-args (-> macro-form butlast last)
          macro-body (-> macro-form last)
          macro-name (first form)
          expansion  (list `macrolet [(list macro-name macro-args macro-body)]
            (concat (list macro-name) args))]
      (macroexpand-1 expansion))))

(defn maybe-macroexpand [form env]
  (if (seq? form)
    (if-let [var* (utils/resolve-var (first form) env)]
      (if (var* :macro?)
        (maybe-macroexpand-var var* form env)))))

(defn macroexpand-1* [form env]
  (or (maybe-macroexpand form env) form))


(defn with-global-ns [local-env]
  (assoc local-env :ns (@env/*env* :ns)))

(defn -parse [form env & rest]
  ; analyzer/-parse gets the namespace from the local env,
  ; where it should get it from the global env/*env*)
  (apply analyzer/-parse form
                         (with-global-ns env)
                         rest))

(defmulti parse (fn [form & rest] (first form)))

(defmethod parse 'in-ns [[_ ns* :as form] env]
  (if (not (get (@env/*env* :namespaces) ns*))
    (swap! env/*env* assoc-in [:namespaces ns*] (new-ns ns*)))
  (swap! env/*env* assoc :ns ns*)
  {:op  :in-ns
   :ns  ns*
   :env (with-global-ns env)})

; TODO: This is really evil
(defmethod parse 'def [[_ name :as form] env & rest]
  (let [ns*              (@env/*env* :ns)
        macro?           (-> name meta :macro)
        macro-form       (nth form 2)
        create-macro-var #(create-macro-var %1 %2 macro-form)
        macro-bindings   {#'analyzer/create-var create-macro-var}]
    (with-bindings (if macro? macro-bindings {})
      (apply -parse form env rest))))

(defmethod parse :default [& rest]
  (apply -parse rest))



(defn analyze*
  ([form] (analyze* form (empty-env)))
  ([form env]
    (with-bindings {#'analyzer/macroexpand-1 macroexpand-1*
                    #'analyzer/parse         parse
                    #'analyzer/create-var    create-var
                    #'analyzer/var?          var?*
                    #'env/*env*              (atom env)}
      {:ast       (analyzer/analyze form env)
       :local-env (empty-local-env)
       :env       @env/*env*})))

(defn analyze [& args]
  ((apply analyze* args) :ast))



; http://stackoverflow.com/a/6840646/1311454
(defn read-all [buffer]
  (let [eof (Object.)]
    (take-while #(not= % eof)
                (repeatedly #(read buffer false eof)))))

(defn analyze-file*
  ([filename] (analyze-file* filename (empty-env)))
  ([filename env]
    (let [forms (-> filename
                    io/file
                    io/reader
                    java.io.PushbackReader.
                    read-all)]
      (loop [unanalyzed forms asts [] env env]
        (if (empty? unanalyzed)
            {:asts (vec asts) :env env}
            (let [analysis (analyze* (first unanalyzed)
                                     env)]
              (recur (rest unanalyzed)
                     (concat asts [(analysis :ast)])
                     (analysis :env))))))))

(defn analyze-file [& args]
  ((apply analyze-file* args) :asts))
