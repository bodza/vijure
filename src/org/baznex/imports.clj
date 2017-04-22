;; Copyright (c) Stuart Sierra, 2008. (See README file for full list.)
;; of contributors. All rights reserved.  The use
;; and distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this
;; distribution.  By using this software in any fashion, you are
;; agreeing to be bound by the terms of this license.  You must not
;; remove this notice, or any other, from this software.

(ns org.baznex.imports
  "Import static Java methods/fields into Clojure"
  (:require clojure.string)
  (:use [clojure.set :only (intersection difference)])
  (:import (java.lang.reflect Method Field Member Modifier)
           (clojure.lang AFn)))

;;;; Old, deprecated stuff

(defmacro import-static
  "Imports the named static fields and/or static methods of the class
  as (private) symbols in the current namespace.

  Example: 
      user=> (import-static java.lang.Math PI sqrt)
      nil
      user=> PI
      3.141592653589793
      user=> (sqrt 16)
      4.0

  Note: The class name must be fully qualified, even if it has already
  been imported.  Static methods are defined as MACROS, not
  first-class fns."
  [class & fields-and-methods]
  (let [only (set (map str fields-and-methods))
        the-class (. Class forName (str class))
        static? (fn [x]
                  (. java.lang.reflect.Modifier
                     (isStatic (. x (getModifiers)))))
        statics (fn [array]
                  (set (map (memfn getName)
                            (filter static? array))))
        all-fields (statics (. the-class (getFields)))
        all-methods (statics (. the-class (getMethods)))
        fields-to-do (intersection all-fields only)
        methods-to-do (intersection all-methods only)
        make-sym (fn [string]
                   (with-meta (symbol string) {:private true}))
        import-field (fn [name]
                       (list 'def (make-sym name)
                             (list '. class (symbol name))))
        import-method (fn [name]
                        (list 'defmacro (make-sym name)
                              '[& args]
                              (list 'list ''. (list 'quote class)
                                    (list 'apply 'list
                                          (list 'quote (symbol name))
                                          'args))))]
    `(do ~@(map import-field fields-to-do)
         ~@(map import-method methods-to-do))))

;;;; Renaming imports

(defn rename
  "Given a map of classes to symbols, import the classes under the symbol
names. For example, an input of {java.lang.Math 'M} would permit expressions
like (M/sqrt 5) inside subsequent top-level forms."
  {:since "1.4.0"}
  [imports]
  (doseq [[^Class cls, ^clojure.lang.Symbol sym] imports]
    (.importClass *ns* sym cls)))

;;;; def-proxied

(defn static?
  "Return true if a class member is static."
  [^Member m]
  (Modifier/isStatic (.getModifiers m)))

(defn tag
  "Provide a suitable value for :tag for use in macros."
  [^Class c]
  (cond
   (nil? c) nil
   (.isArray c) (.getName c)
   :else (symbol (.getName c))))

(defn emit-cast
  "Emit expression with casting or type hinting as appropriate."
  [^Class c, expr]
  (cond
   (nil? c) expr
   (= c Void/TYPE) (throw (IllegalArgumentException. "Cannot cast to void"))
   (.isPrimitive c) (list (tag c) expr)
   :else (with-meta expr {:tag (tag c)})))

(defn ^:internal priv-sym
  "Produce a private name (with minimal docs) for imported statics."
  [^Class cls, ^String name]
  (vary-meta (symbol name)
             assoc
             :private true
             :doc (str (.getCanonicalName cls) "/" name " via def-proxied")))

;; Sample signature:
;; {:arity 3
;; ?:args [Long/TYPE Double/TYPE String] ; optional
;; }

(defn ^:internal extract-signature
  "Given a method, return the signature as a map of :arity (integer) and
:args (sequence of the declared parameter classes of the static method.)

:args is intended for use when the dispatch is unambiguous but the actual
parameters of the static method are narrower than what invoke can provide."
  [^Method meth]
  (let [par-actual (vec (seq (.getParameterTypes meth)))]
    {:arity (count par-actual)
     :args par-actual}))

(defn ^:internal collapse-sigs
  "Collapse signatures together that must use the same .invoke."
  [sigs]
  (for [[ary sgroup] (group-by :arity sigs)
        :let [sample (first sgroup)]]
    (if (= 1 (count sgroup))
      sample
      {:arity ary, :args nil})))

(defn ^:internal invocation
  "Produce a single invocation arity implementation from a signature."
  [^Class cls, ^String name, {:keys [arity args] :as sig}]
  (when (> arity 20)
    (throw (RuntimeException.
            "def-proxied does not yet support methods with > 20 params")))
  (let [proxargs (repeatedly arity (partial gensym 'p_))]
    `([~@proxargs]
        (. ~(symbol (.getName cls))
           ~(symbol name)
           ~@(if (seq args)
               (map #(emit-cast %2 %1) proxargs args)
               proxargs)))))

(defn ^:internal proxy-one-method
  "Produce a proxy form for a collection of static methods with the same name."
  [^Class cls, ^String mname, meths]
  (let [sigs (collapse-sigs (map extract-signature meths))]
    `(proxy [AFn] []
       (~'invoke
         ~@(for [sig sigs]
             (invocation cls mname sig))))))

(defmacro proxied
  "Return a function that wraps the specified class method (with the same
characteristics as def-proxied.)"
  [class-sym method-sym]
  (if-let [cls (resolve class-sym)]
    (let [mname (name method-sym)
          meths (filter (fn [^Method m]
                          (and (static? m) (= (.getName m) mname)))
                        (.getMethods cls))]
      (when (empty? meths)
        (throw (IllegalArgumentException.
                (format "Could not find static method '%s' in %s" mname cls))))
      ;; emit bare proxy form
      (proxy-one-method cls mname meths))
    (throw (ClassNotFoundException.
            (format "Could not resolve class %s for proxying." class-sym)))))

(defn ^:internal emit-statics-clause
  "Emit def-proxied syntax for one clause."
  [class-sym & fields-and-methods]
  (if-let [cls (resolve class-sym)]
    (let [only (set (map str fields-and-methods))
          todo? (fn [mem]
                  (and (static? mem)
                       (contains? only (.getName mem))))
          fields (filter todo? (.getFields cls))
          methods-by-name (group-by #(.getName ^Method %)
                                    (filter todo? (.getMethods cls)))]
      ;; confirm that discovered fields and methods form a partition of the
      ;; requested static members
      (let [mnames (set (keys methods-by-name))
            fnames (set (map #(.getName ^Field %) fields))]
        (when-let [ambig (seq (intersection fnames mnames))]
          (throw (IllegalArgumentException.
                  (format (str "def-proxied given ambiguous field "
                               "and method names for class %s: %s")
                          (.getName cls)
                          (clojure.string/join ", " ambig)))))
        (when-let [missing (seq (difference only fnames mnames))]
          (throw (IllegalArgumentException.
                  (format (str "def-proxied did not find these fields "
                               "or methods in class %s: %s")
                          (.getName cls)
                          (clojure.string/join ", " missing))))))
      ;; OK, we're good to go
      `(do ~@(for [fld fields]
               `(def ~(priv-sym cls (.getName fld))
                  (. ~(symbol (.getName cls)) ~(symbol (.getName fld)))))
           ~@(for [[mname meths] methods-by-name]
               `(def ~(priv-sym cls mname)
                  ~(proxy-one-method cls mname meths)))
           nil)) ;; TODO: A more interesting return value?
    (throw (ClassNotFoundException.
            (str "Could not resolve class " class-sym " for proxying.")))))

(defmacro def-proxied
  "Proxy the named static fields and/or static methods of the class
as private vars in the current namespace.

Arguments are one or more clauses. A clause is a list containing a
classname symbol followed by one or more static field and method symbols.
For convenience, the arguments can instead be the contents of a single clause.

Classnames will be resolved according to any imports that have taken effect
by expansion time.

Example:
  user=> (def-proxied Math PI sqrt)
    nil
  user=> (map sqrt [16 PI])
    (4.0 1.7724538509055159)
  user=> (doc sqrt)
    -------------------------
    user/sqrt
      java.lang.Math/sqrt via def-proxied
    nil
  user=> (def-proxied (String valueOf) (Double parseDouble POSITIVE_INFINITY))
    nil
  user=> (valueOf (parseDouble \"+5.6\"))
    \"5.6\"

Primitive boxing will be used with all methods. Reflection will only
be used where two overloads share an arity."
  [& args]
  (when (empty? args)
    (throw (IllegalArgumentException. "def-proxied not given any clauses")))
  (let [clauses (if (sequential? (first args)) args (list (seq args)))]
    ;; Reserve vectors for future syntax
    (when-not (and (every? sequential? clauses)
                   (every? (complement associative?) clauses))
      (throw (IllegalArgumentException.
              "At least one clause was not a list.")))
    `(do
       ~@(for [clause clauses]
           (apply emit-statics-clause clause))
       nil)))
