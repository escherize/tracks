(ns tracks.core-test
  (:require [clojure.test :refer :all]
            [tracks.core :refer :all]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as g]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]))

(def scalar (g/one-of [g/int g/string-alphanumeric g/keyword]))

(def ^:dynamic *run-times* 20)

(def shallow-data-gen
  (g/one-of
   [(g/vector scalar) (g/list scalar) (g/map scalar scalar)]))

(def shallow-map-gen
  (g/such-that not-empty (g/map scalar scalar)))

(def map-gen
  (g/map g/keyword shallow-data-gen))

(def recursive-data-gen
  (g/recursive-gen (fn [inner] (g/one-of [(g/vector inner) (g/list inner) (g/map inner inner)]))
                   shallow-data-gen))

(defspec simple-paths
  *run-times*
  (prop/for-all [m shallow-map-gen]
                (let [[in out] ((juxt identity paths) m)]
                  (= (first (keys out)) (get-in in (first (vals out)))))))

(deftest deeper-map-path
  (let [in-map {:a ["one" "two"]}
        p (paths in-map)]
    (is (= p
           {"one" [:a 0]
            "two" [:a 1]}))))

(deftest track-works
  (testing "can move keys"
    (is (= {:b "!!"}
           ((track {:a 1} {:b 1})
            {:a "!!"}))))

  (testing "swap vectors"
    (is (= [:a :b]
           ((track [0 1] [1 0])
            [:b :a])))
    (is (= [:a :c :b]
           ((track [0 1 2] [0 2 1])
            [:a :b :c]))))

  (testing "can move + swap vectors"
    (let [a-to-b-and-reverse (track {:a [0 1]} {:b [1 0]})]
      (is (= {:b [:one :zero]}
             (a-to-b-and-reverse
              {:a [:zero :one]}))))))
