# tracks

![Converging Tracks](https://raw.githubusercontent.com/escherize/tracks/master/tracks.jpg)

This library simplifies transformations of Clojure datastructures. Instead of saying how to do a transformation, track works by allowing you to create those transformations by __example__.  This makes writing glue code that takes giant maps of one shape and transforms them into another *dead simple*.

## Examples

We can do simple transformations on maps with track:

``` clojure
(def swap-a-b (track {:a 1 :b 2} {:a 2 :b 1}))
(swap-a-b {:a 100 :b 3000})

;;=> {:a 3000 :b 100}

(def must-go-deeper
  (track {0 0 1 1 2 2 3 3}
  {:a 0 :b {:c 1 :d {:e 2 :f {:g 3}}}}))

(deep-ways {0 "first" 1 "sec" 2 "therd" 3 "feor"})
;; => {:item {:a "BBB", :b "HHH", :c "SSS"}}
```

or more complicated ones:

``` clojure
((track {:a [0 1]} {:b [1 0]}) {:a [:zero :one]})
;; => {:b [:one :zero]}
```

Here we do something weird, understandably and quickly:

```clojure
(def deep-ways
  (track {0 "can" 1 "use" 2 "any" 3 "scalar value."}
         {:a "can" :b {:c "use" :d {:e "any" :f {:g "scalar value."}}}}))

(deep-ways {0 "first" 1 "sec" 2 "therd" 3 "feor"})
;; => {:a "first" :b {:c "sec" :d {:e "therd" :f {:g "feor"}}}}


;; Without track this is not a clear concise operation:
(let [input {0 "first" 1 "sec" 2 "therd" 3 "feor"}]
     {:a (get input 0)
      :b {:c (get input 1)
          :d {:e (get input 2)}
              :f {:g (get input 3)}}})

```

`track` greatly simplifies rotating values:

```clojure

(def move-players
  (tracks {:active-player 1 :players [2 3 4]}
          {:active-player 2 :players [3 4 1]}))

(defonce game (atom {:active-player {:name "A"} ;;<- note the more complex leaf!
                     :players [{:name "B"}
                               {:name "C"}
                               {:name "D"}]}))

(swap! game move-players)
;;=>  {:active-player {:name "B"}
;;     :players [{:name "C"}
;;               {:name "D"}
;;               {:name "A"}]}

(swap! game move-players)
;;=>  {:active-player {:name "C"}
;;     :players [{:name "D"}
;;               {:name "A"}
;;               {:name "B"}]}


(swap! game move-players)
;;=>  {:active-player {:name "D"}
;;     :players [{:name "A"}
;;               {:name "B"}
;;               {:name "C"}]}

```

If common values don't exist between in and out, they are ignored (`:z` and `"??"` here).

``` clojure
(def zed (track {:a [0 1] :z "??"} {:b [1 0]}))

(zed {:a [:zero :one]})
;; => {:b [:one :zero]}

(zed {:a ["ONE" "TWO"] :z "this will be ignored" :c "as will this."})
;;=> {:b ["TWO" "ONE"]}

```

### Partial updating values with track

Often we only want to update part of a datastructure. tracks make it possible:

``` clojure

(def transfer-a-to-b (track {:a 1} {:b 1}))

(transfer-a-to-b {:a 1 :c 2})
;;=> {:b 1 :c 2}


(transfer-a-to-b {:a 1 :c 2 :d 3})
;;=> {:b 1 :c 2 :d 3}

```

Note well: key paths not operated on by the function returned by tracks aren't touched.


### Using functions with track

Sometimes we want more than a pure transformation. That's why track lets you supply a map explaining what functions to apply to what leaf nodes.

``` clojure
(def swap-and-inc
  (track
    {:here 1}
    {:now {:its {:here [1 "<- and its one larger."]}}}
    {1 inc} ;;<- means call inc on the value at position 1
    ))
(swap-and-inc {:here 100})
;;=> {:now {:its {:here [101 "<- and its one larger."]}}}

```

## License

Copyright © 2016 Bryan Maass

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
