(ns tracks.core-test
  (:require [clojure.test :refer :all]
            [tracks.core :as t :refer :all :exclude [let]]
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
  (g/such-that #(and (not-empty %)
                     (= (count (vals %))
                        (count (set (vals %)))))
               (g/map scalar scalar)
               ;; 100 tries not 10.
               100))

(defspec shallow-maps-shuffled-keys
  *trial-count*
  (prop/for-all [m shallow-map-gen]
                (let [map-two (zipmap (shuffle (keys m)) (vals m))]
                  (= map-two ((track m map-two) m)))))
(deftest t-let
  (testing "multiple bindings"
    (is (= 2 (t/let [{:a a} {:a 1}
                     b      (+ a a)]
               b)))))

(deftest tracks-works
  (testing "can move keys"
    (is (= {:b "!!"}
           ((track {:a value} {:b value})
            {:a "!!"}))))

  (testing "swap vectors"
    (is (= [:a :b] ((track [f s] [s f]) [:b :a])))
    (is (= [:a :c :b] ((track [f s t] [f t s]) [:a :b :c]))))

  (testing "can move + swap vectors"
    (let [a-to-b-and-reverse (track {:a [f s]} {:b [s f]})]
      (is (= {:b [1 0]}
             (a-to-b-and-reverse {:a [0 1]}))))))

(deftest rotation
  (let [rotate-players (track {:active-player a :players [b c d]}
                            {:active-player b :players [c d a]})
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
  (is (= "Hi Hello!!!???"
         (t/let [{:a hi :b hello :c [one two]} {:a "Hi" :b "Hello" :c ["!!" "???"]}
                 {:punk punk} {:punk "!"}]
           (str hi " " hello punk one two))))

  (is (= "Hello World!"
         (let [bang "!"]
           (t/let [{:a hi :b hello} {:a "Hello" :b "World"}
                   {:punk punk} {:punk bang}]
             (str hi " " hello punk)))))

  (testing "going deeper"
    (is (= "Hello World!"
           (let [bang "!"]
             (t/let [{:a [_ _ _ _ hi]
                      :b {:d {:e {:f [_ _ hello]}}}}
                     {:a [0 1 2 3 "Hello"]
                      :b {:d {:e {:f ["ignore" "me" "World"]}}}}
                     {:punk punk} {:punk bang}]
               (str hi " " hello punk))))))
  (is (nil? (t/let [])))
  (is (= 1 (t/let [a 1] a)))
  (is (nil? (t/let [a 10]))))

(deftest multi-track
  (is (= [1 1 1] ((track [x] [x x x]) [1])))
  (is (= [2 2 2] ((track [x] [x x x]) [2])))
  (is (= [1 0 1 0] ((track [x y] [x y x y]) [1 0])))
  (is (= {:b "ayee", :c "ayee"}
         ((track {:a a} {:b a :c a}) {:a "ayee"})))
  (is (= {:a "ayee+ayee", :b "ayee", :c "ayee"}
         ((track {:a a} {:a (str a "+" a) :b a :c a}) {:a "ayee"}))))

(deftest deftrack-test
  (try (deftrack ab {:a pop} {:b pop})
       (is (= {:b "???"} (ab {:a "???"})))
       (finally (ns-unmap *ns* 'ab))))
