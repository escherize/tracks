(ns tracks.core-test
  (:require [clojure.test :refer :all]
            [tracks.core :refer :all]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as g]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]))

(deftest ->m-test
  (testing "adds metadata to top level data"
    (doseq [ex [ [1]  [1 2]  [[1]]  [[[[2]]]]
                '(1) '(1 2) '([1]) '([[[2]]])]]
      (is (= (type ex)
             (:type (meta (->m ex))))))))

(def shallow-data-gen
  (let [many [g/int g/string-alphanumeric g/keyword]]
    (g/one-of [(g/vector (g/one-of many))
               (g/list (g/one-of many))
               (g/map (g/one-of many)
                      (g/one-of many))])))

(def recursive-data-gen
  (g/recursive-gen (fn [inner] (g/one-of [(g/vector inner) (g/list inner) (g/map inner inner)]))
                   shallow-data-gen))

(defspec shallow-m-inverse-is-m
  20
  (prop/for-all [d shallow-data-gen]
                (= d (<-m (->m d)))))

(defspec deep-m-inverse-is-m
  20
  (prop/for-all [d recursive-data-gen]
                (= d (<-m (->m d)))))
