(defproject tracks "0.1.4"
  :description "Example based function generation"
  :url "https://github.com/escherize/tracks"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]]
  :profiles {:dev {:dependencies [[org.clojure/test.check "0.9.0"]]
  				   :main tracks.core}})
