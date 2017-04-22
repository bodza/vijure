(defproject vijure "x.y.z"
    :dependencies [[org.clojure/clojure "1.8.0"]
                   [org.clojure/core.async "0.2.374"]
                   [com.github.jnr/jnr-ffi "2.0.9"]
                 #_[com.github.jnr/jffi #_"1.2.8" "1.2.12"]
                 #_[org.ow2.asm/asm #_"5.0.4" "5.1"]]
    :plugins [[lein-try "0.4.3"]
            #_[venantius/ultra "0.4.1"]]
;   :global-vars {*warn-on-reflection* true}
    :jvm-opts ["-Xmx12g"]
;   :javac-options ["-g"]
    :source-paths ["src"] :java-source-paths ["src"] :resource-paths ["resources"] :test-paths ["src"]
    :main vijure.core
    :aliases {"vijure" ["run" "-m" "vijure.core"]})
