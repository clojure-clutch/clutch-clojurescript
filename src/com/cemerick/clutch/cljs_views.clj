(ns com.cemerick.clutch.cljs-views
  (:require [cljs.closure :as closure]))

; TODO
; * no ClojureScript library
; * large code footprint, even for the simplest of views
; ** probably not anything we can do about that in general (e.g. use `map`, get all the code for collections, seqs, etc)
; * namespaces and function names and gensyms might be tricky:
; ** how long-lived are the view servers?  How about in non-Apache CouchDB (cloudant)?
; ** def'ing named vars might cause collisions, unintentional use of different versions of required cljs code, etc
; ** maybe not a problem; see what large view implementations do in terms of namespacing

(defn- expand-anon-fn
  "Compiles a single anonymous function body in a dummy namespace, with a gensym'ed
   name. Separate from the general case because cljs doesn't include cljs.core and
   the goog stuff if a namespace is not specified, and advanced gClosure
   optimization drops top-level anonymous fns."
  [fnbody]
  (assert (and (seq fnbody)
               (= 'fn (first fnbody)))
          "Simple ClojureScript views must be an anonymous fn, e.g. (fn [doc] …)")
  (let [namespace (gensym)
        name (with-meta (gensym) {:export true})]
    [{:main (symbol (str namespace) (str name))}
     [(list 'ns namespace)
      (list 'def name fnbody)]]))

(defn view*
  "Compiles a body of "
  [options & body]
  (let [[options' body] (if (== 1 (count body))
                          (expand-anon-fn (first body))
                          [nil (vec body)])
        options (merge {:optimizations :advanced :pretty-print false}
                       options'
                       options)]
    (assert (:main options) "Must specify a fully-qualified entry point fn via :main option")
    (str (closure/build body options)
         "return " (-> options :main namespace) \. (-> options :main name))))

(defn- closure
  "Wraps the provided string of code in a closure.  This isn't strictly needed right now
   (i.e. circa couchdb ~1.0.2), but couchdb 1.2 will begin to require that view/filter/validation
   code be defined in a single _expression_.  This is necessary to ensure that we're producing a
   single expression, given Google Closure's penchant for lifting
   closures to the top level of advanced-optimized code (even if the code provided to it is entirely
   contained within a closure itself, making lifting somewhat pointless AFAICT)."
  [code]
  (str "(function () {"
       code
       "})()"))

(def view* (comp closure view*))

(defmacro view
  [& body]
  (let [[options body] (if (map? (first body))
                         [(first body) (rest body)]
                         [nil body])]
    (apply view* options body)))

#_(println (view* nil
                  '(fn [doc]
                     (when-let [key (and (aget doc "order")
                                         (subs (aget doc "support-through") 0 10))]
                       (js/emit (apply array [key (aget doc "qty")]) nil)))))


