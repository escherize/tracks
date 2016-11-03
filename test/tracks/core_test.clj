(ns tracks.core-test
  (:require [clojure.test :refer :all]
            [tracks.core :as t]
            [tracks.tracks :as tracks]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as g]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]))

(def scalar (g/one-of [g/char g/int g/string-alphanumeric g/keyword]))

(def ^:dynamic *trial-count* 100)

(def shallow-data-gen
  (g/one-of
   [(g/vector scalar) (g/list scalar) (g/map scalar scalar)]))

(def shallow-map-gen
  (g/such-that #(and (not-empty %)
                     (= (count (vals %))
                        (count (set (vals %)))))
               (g/map scalar scalar)
               ;; 100 tries not 10.
               100))

(defspec simple-paths
  *trial-count*
  (prop/for-all [m shallow-map-gen]
                (let [[in out] ((juxt identity (comp tracks/paths tracks/->m)) m)]
                  (= (first (keys out)) (get-in in (first (vals out)))))))

(deftest tracks-is-non-destructive
  (is (= {:b 1 :c 2}
         ((t/track {:a 1} {:b 1}) {:a 1 :c 2})))
  (is (= {:b 1 :c 2 :d 3}
         ((t/track {:a 1} {:b 1}) {:a 1 :c 2 :d 3}))))

(defspec complex-maps
  *trial-count*
  (prop/for-all [in shallow-map-gen]
                (let [leafs (keys (tracks/paths (tracks/->m in)))
                      out-paths (repeatedly (count leafs) #(g/sample scalar))
                      out (->> (map #(assoc-in {} %1 %2) out-paths leafs)
                               (apply merge))
                      in->out (t/track in out)]
                  (= out (in->out in)))))

(defspec shallow-maps-shuffled-keys
  *trial-count*
  (prop/for-all [m shallow-map-gen]
                (let [map-two (zipmap (keys m) (shuffle (vals m)))]
                  (= map-two ((t/track m map-two) m)))))

(deftest deeper-map-path
  (let [in-map {:a ["one" "two"]}
        p (tracks/paths (tracks/->m in-map))]
    (is (= p {"one" [:a 0]
              "two" [:a 1]}))))

(deftest track-works
  (testing "can move keys"
    (is (= {:b "!!"}
           ((t/track {:a 1} {:b 1})
            {:a "!!"}))))

  (testing "swap vectors"
    (is (= [:a :b]
           ((t/track [0 1] [1 0])
            [:b :a])))
    (is (= [:a :c :b]
           ((t/track [0 1 2] [0 2 1])
            [:a :b :c]))))

  (testing "can move + swap vectors"
    (let [a-to-b-and-reverse (t/track {:a [0 1]} {:b [1 0]})]
      (is (= {:b [:one :zero]}
             (a-to-b-and-reverse
              {:a [:zero :one]}))))))

(deftest track-with-lists
  (testing "swap lists"
    (is (= '(:a :b)
           ((t/track '(0 1) '(1 0))
            '(:b :a))))
    (is (= '(:a :c :b)
           ((t/track '(0 1 2) '(0 2 1))
            '(:a :b :c))))))

(deftest rotation
  (let [rotate-players (t/track {:active-player 1 :players [2 3 4]}
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

(deftest testing-let
  (is (= "Hello World!"
         (t/let [{:a hi :b hello} {:a "Hello" :b "World"}
                 {:punk punk} { :punk "!"}]
           (str hi " " hello punk))))

  (is (= "Hello World!"
         (let [bang "!"]
           (t/let [{:a hi :b hello} {:a "Hello" :b "World"}
                   {:punk punk} { :punk bang}]
             (str hi " " hello punk)))))

  (testing "going deeper"
    (is (= "Hello World!"
           (let [bang "!"]
             (t/let [{:a [_ _ _ _ hi]
                      :b {:d {:e {:f [_ _ hello]}}}}
                     {:a [0 1 2 3 "Hello"]
                      :b {:d {:e {:f ["ignore" "me" "World"]}}}}
                     {:punk punk} { :punk bang}]
               (str hi " " hello punk))))))

  (is (nil? (t/let [])))

  (is (= 1 (t/let [a 1] a)))

  (is (nil? (t/let [a 1]))))
