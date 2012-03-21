# ClojureScript views for CouchDB

Write your views/filters/validators in Clojure(Script), run the results in CouchDB or Cloudant — no special view servers, no special configuration, no JavaScript!

## Status

Super-experimental, but works.

## "Installing"

clutch-clojurescript is available in clojars.  Add it to your Maven project's `pom.xml`:

```xml
<dependency>
  <groupId>com.cemerick.clutch</groupId>
  <artifactId>clutch-clojurescript</artifactId>
  <version>0.0.1</version>
</dependency>
```

or your leiningen/cake `project.clj`:

```clojure
[com.cemerick.clutch/clutch-clojurescript "0.0.1"]
```

## Usage

This is an extension of [Clutch](http://github.com/ashafa/clutch), the CouchDB client library and view server for Clojure.

1. Load Clutch as you usually would.
2. Make sure you've loaded clutch-clojurescript, e.g. `(require 'com.cemerick.clutch.cljs-views)`, or `(:require com.cemerick.clutch.cljs-views)` in your `ns` form.  Note that you'll never need to directly access anything defined in `com.cemerick.clutch.cljs-views` — you only need to load the namespace to allow it to register a multimethod implementation in clutch's core API.
3. Save a view using Clutch's usual `save-view` function:

```clojure
(with-db "your_database"
  (save-view "design_document_name"
    (view-server-fns :cljs
      {:your-view-name {:map (fn [doc]
                               (js/emit (aget doc "_id") nil))}})))
```

That's an example of a silly view, but should demonstrate the general pattern.  Note the `js/emit` function; after ClojureScript compilation, this results in a call to the `emit` function defined by the standard CouchDB Javascript view server for emitting an entry into the view result.  Follow the same conventions for reduce functions, filter functions, validator functions, etc.

Your views can utilize larger codebases; just include your "top-level" ClojureScript forms in a vector:

```clojure
(with-db "your_database"
  (save-view "design_document_name"
    (view-server-fns {:language :cljs
                      :main 'couchview/main}
      {:your-view-name {:map [(ns couchview)
                              (defn concat
                                [id rev]
                                (str id rev))
                              (defn ^:export main
                                [doc]
                                (js/emit (concat (aget doc "_id") (aget doc "_rev")) nil))]}})))
```

The `ns` form here can require other ClojureScript files on your classpath, refer to macros, etc.  When using this longer form, remember to do three things:

1. You must provide a map of options to `view-server-fns`; `:cljs` becomes the `:language` value here.
2. Specify the "entry point" for the view function via the `:main` slot, `'couchview/main` here.  This must correspond to an exported, defined function loaded by some ClojureScript, either in your vector literal of in-line ClojureScript, or in some ClojureScript loaded via a `:require`.
3. Ensure that your "entry point" function is exported; here, `main` is our entry point, exported via the `^:export` metadata.

These last two points are required because of the default ClojureScript compilation option of `:advanced` optimizations.

### Compilation options

The `view-server-fns` macro provided by Clutch (version `0.3.0-SNAPSHOT` and later) takes as its first argument some options to pass along to the view transformer specified in that options map's `:language` slot.  The clutch-clojurescript view transformer passes this options map along to the ClojureScript/Google Closure compiler, with defaults of:

```clojure
{:optimizations :advanced
 :pretty-print false}
```

So you can e.g. disable `:advanced` optimizations and turn on pretty-printing by passing this options map to `view-server-fns`:

```clojure
{:optimizations :simple
 :pretty-print true
 :language :cljs}
```

### Internals

If you really want to see what Javascript ClojureScript is generating for your view function(s), call `com.cemerick.clutch.cljs-views/view` with an options map as described above (`nil` to accept the defaults) and either an anonymous function body or vector of ClojureScript top-level forms. 

## Caveats

* ClojureScript / Google Closure produces a _very_ large code footprint, even for the simplest of view functions.  This is apparently an item of active development in ClojureScript.
** In any case, the code size of a view function string should have little to no impact on runtime performance of that view.  The only penalty to be paid should be in view server initialization, which should be relatively infrequent.  Further, the vast majority of view runtime is dominated by IO and actual document processing, not the loading of a handful of JavaScript functions.

## Need Help?

Ping `cemerick` on freenode irc or twitter if you have questions or would like to contribute patches.

## License

Copyright © 2011-2012 [Chas Emerick](http://cemerick.com) and other
contributors.

Licensed under the EPL. (See the file epl-v10.html.)
