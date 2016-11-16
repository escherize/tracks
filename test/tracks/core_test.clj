(ns tracks.core-test
  (:require [clojure.test :refer :all]
            [tracks.core :as t]
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
                (let [map-two (zipmap (keys m) (shuffle (vals m)))]
                  (= map-two ((t/track m map-two) m)))))

(deftest track-let
  (testing "multiple bindings"
    (is (= 2 (t/let [{:a a} {:a 1}
                     b      (+ a a)]
               b)))))

(deftest track-works
  (testing "can move keys"
    (is (= {:b "!!"}
           ((t/track {:a value} {:b value})
            {:a "!!"}))))

  (testing "swap vectors"
    (is (= [:a :b]
           ((t/track [f s] [s f])
            [:b :a])))
    (is (= [:a :c :b]
           ((t/track [f s t] [f t s])
            [:a :b :c]))))

  (testing "can move + swap vectors"
    (let [a-to-b-and-reverse (t/track {:a [f s]} {:b [s f]})]
      (is (= {:b [:one :zero]}
             (a-to-b-and-reverse
              {:a [:zero :one]}))))))

(deftest rotation
  (let [rotate-players (t/track {:active-player a :players [b c d]}
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

(deftest track-is-fn

  (is (= 4000
         ((t/track {:a a}
                   (clojure.core/let [x (+ a a)]
                     (* x a x)))
          {:a 10})))

  (is (= [43 53 63 46 56 66 49 59 69]
         (mapv
          (t/track {:a {:a {:a a}} :b {:b {:b {:b b}}}} (+ a a a b))
          (for [a [1 2 3] b [40 50 60]]
            {:a {:a {:a a}} :b {:b {:b {:b b}}}})))))

(deftest multi-track
  (is (= [1 1 1] ((t/track [x] [x x x]) [1])))

  (is (= [2 2 2] ((t/track [x] [x x x]) [2])))

  (is (= {:b "ayee", :c "ayee"}
         ((t/track {:a a} {:b a :c a}) {:a "ayee"}))))

(deftest deftrack
  (t/deftrack swap-a-and-b
    {:a pop}
    (println pop)
    {:b pop})

  (is (= {:b "???"}
         (swap-a-and-b {:a "???"}))))

;; deprecated - tracks does not work with lists.
#_(deftest track-with-lists
    (testing "swap lists"
      (is (= '(:a :b)
             ((t/track (a b) (b a))
              '(:b :a))))
      (is (= '(:a :c :b)
             ((t/track (a b c) (a c b))
              '(:a :b :c))))))

;; deprecated - tracks is destructive, but you can write your own.
#_(deftest tracks-is-non-destructive
    (is (= {:b 1 :c 2} ;; <- notice c is unchanged.
           ((t/track {:a a} {:b a}) {:a 1 :c 2})))
    (is (= {:b 1 :c 2 :d 3}
           ((t/track {:a a} {:b a}) {:a 1 :c 2 :d 3}))))
