# tracks

![Converging Tracks](https://raw.githubusercontent.com/escherize/tracks/master/tracks.jpg)

This library is intended to simplify transformations of Clojure datastructures. Instead of saying what we want done to them in a sequential order, track lets you create functions by __example__.

So you setup a track from what you have to what you want and track does the rest.

## Usage

We can do simple transformations with track:

``` clojure
(def swap-a-b (tracks {:a 1 :b 2} {:a 2 :b 1}))

(swap-a-b {:a 100 :b 3000})

;;=> {:a 3000 :b 100}
```

or more complicated ones:

``` clojure
((track {:a [0 1]} {:b [1 0]}) {:a [:zero :one]})

;; => {:b [:one :zero]}
```
Here we do something understandably and quickly:

```clojure
(def deep-ways
  (track {0 "can" 1 "use" 2 "any" 3 "scalar value."}
         {:a "can" :b {:c "use" :d {:e "any" :f {:g "scalar value."}}}}))

(deep-ways {0 "first" 1 "sec" 2 "therd" 3 "feor"})
;; => {:a "first" :b {:c "sec" :d {:e "therd" :f {:g "feor"}}}}
```


This greatly simplifies some arbitrary logic when it comes to swapping around things in the code:
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

`tracks` works by finding paths to identitical values in the inputs and automatically producing a function that will make those changes.


## Caveats

Since sets are not ordered, this library doesn't support transformations on them. Also, using track with lists is currently unsupported.

## License

Copyright © 2016 Bryan Maass

Distributed under the Eclipse Public License either version 1.0 or (at
                                                                    your option) any later version.
