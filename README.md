# tracks

![Converging Tracks](https://raw.githubusercontent.com/escherize/tracks/master/tracks.jpg)

This library is intended to simplify transformations of Clojure datastructures. Instead of saying what we want done to them in a sequential order, track lets you create functions by __example__.  This makes writing glue code that takes giant maps of one shape and transforms them *dead simple*.

It works by finding paths to identitical values in the inputs and automatically producing a function that will make those changes.

So, setup a track from what you have to what you want and track does the rest!

## Usage

We can do simple transformations on maps track:

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

If the values don't exist, they are ignored (`:z` and `"??"` here).

``` clojure
(def zed (track {:a [0 1] :z "??"} {:b [1 0]}))

(zed {:a [:zero :one]})
;; => {:b [:one :zero]}

(zed {:a ["ONE" "TWO"] :z "this will be ignored" :c "as will this."})
;;=> {:b ["TWO" "ONE"]}

```

### Using functions with track
#### With maps:
Sometimes we want a bit more than a pure transformation.  That's why track lets you supply a map explaining what functions to apply to what leaf nodes.

``` clojure
(def swap-and-inc
  (track
    {:here 1}
    {:now {:its {:here [1 "<- and its one larger."]}}}
    {1 inc} ;;<- means call in on 1 as it's moved.
    ))
(swap-and-inc {:here 100})
;;=> {:now {:its {:here [101 "<- and its one larger."]}}}

```


## License

Copyright © 2016 Bryan Maass

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
