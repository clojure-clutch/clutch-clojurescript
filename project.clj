(defproject com.cemerick/clutch-clojurescript "0.0.1-SNAPSHOT"
  :description "Experimental support for defining CouchDB views in ClojureScript via Clutch."
  :url "http://github.com/cemerick/clutch-clojurescript"
  :dependencies [[com.ashafa/clutch "0.3.0-SNAPSHOT"]]
  :extra-classpath-dirs ["clojurescript-bin/compiler.jar" "clojurescript-bin/goog.jar"]
  :dev-dependencies [[lein-clojars "0.7.0"]])

(require 'leiningen.jar
         '[clojure.java.io :as io])

(defmethod leiningen.jar/copy-to-jar :jar
  [project jar-os spec]
  (let [f (java.util.jar.JarFile. (:jar spec))]
    (doseq [entry (enumeration-seq (.entries f))
            :when (not (-> entry .getName (.startsWith "META-INF")))]
      (.putNextEntry jar-os entry)
      (io/copy (.getInputStream f entry) jar-os))))

(add-hook #'leiningen.jar/filespecs
          (fn [filespecs project & args]
            (concat (apply filespecs project args)
                    (->> project
                      :extra-classpath-dirs
                      (map (fn [path]
                             {:type :jar :jar path}))))))
