(ns tracks.core-test
  (:require [clojure.test :refer :all]
            [tracks.core :refer :all]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as g]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]))

(def scalar (g/one-of [g/char g/int g/string-alphanumeric g/keyword]))

(def ^:dynamic *trial-count* 50)

(def shallow-data-gen
  (g/one-of
   [(g/vector scalar) (g/list scalar) (g/map scalar scalar)]))

(def shallow-map-gen
  (g/such-that not-empty (g/map scalar scalar)))

(defspec simple-paths
  *trial-count*
  (prop/for-all [m shallow-map-gen]
    (let [[in out] ((juxt identity (comp paths ->m)) m)]
      (= (first (keys out)) (get-in in (first (vals out)))))))

(defspec complex-maps
  *trial-count*
  (prop/for-all [in-map shallow-map-gen]
    (let [in in-map
          leafs (keys (paths (->m in)))
          out-paths (repeatedly (count leafs) #(g/sample scalar))
          out (->> (map #(assoc-in {} %1 %2) out-paths leafs)
                   (apply merge))
          in->out (tracks in out)]
      (= out (in->out in)))))

(defspec shallow-maps-shuffled-keys
  *trial-count*
  (prop/for-all [m shallow-map-gen]
    (let [map-two (zipmap (keys m) (shuffle (vals m)))]
      (= map-two ((track m map-two) m)))))

(deftest deeper-map-path
  (let [in-map {:a ["one" "two"]}
        p (paths (->m in-map))]
    (is (= p {"one" [:a 0]
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

(deftest track-with-lists
  (testing "swap lists"
    (is (= '(:a :b)
           ((track '(0 1) '(1 0))
            '(:b :a))))
    (is (= '(:a :c :b)
           ((track '(0 1 2) '(0 2 1))
            '(:a :b :c))))))

(deftest rotation
  (let [rotate-players (tracks {:active-player 1 :players [2 3 4]}
                               {:active-player 2 :players [3 4 1]})
        initial-game {:active-player {:name "A"} ;;<- note the more complex leaf!
                      :players [{:name "B"}
                                {:name "C"}
                                {:name "D"}]}
        game (atom initial-game)]
    (is (= initial-game @game))
    (swap! game rotate-players)
    (is (= {:active-player {:name "B"}
            :players [{:name "C"}
                      {:name "D"}
                      {:name "A"}]} @game))
    (swap! game rotate-players)
    (swap! game rotate-players)
    (swap! game rotate-players)
    (is (= initial-game @game))))
