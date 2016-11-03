# tracks

## Example based coding

![Converging Tracks](https://raw.githubusercontent.com/escherize/tracks/master/tracks.jpg)

> Be the change that you wish to see in the world

> -- Mahatma Gandhi

## Usage

Add the following line to your leiningen dependencies:

[![Clojars Project](https://img.shields.io/clojars/v/tracks.svg)](https://clojars.org/tracks)

Require tracks in your namespace header:


    (:require [tracks.core :as t :refer [track]])


## Rationale

Often we find ourselves with large maps that are uncomfortable to reason about.

This library simplifies transformations and destructuring of Clojure datastructures. Instead of describing how to do a transformation, tracks allows you to create those transformations by __example__.  This makes writing glue code that takes giant maps of one shape and transforms them into another *dead simple*.


## Examples

`track` returns a function that sets up "rails" which turn its first argument into its second argument.

Below, the function returned by `track` will move the value at `:a` to `:b`, and the value from `:b` to `:a`:

``` clojure
(def swap-a-b (track {:a 1 :b 2}
                     {:a 2 :b 1}))
(swap-a-b {:a 100 :b 3000})

;;=> {:a 3000 :b 100}

```

You might say it's a bit like rename-keys, but we can move positions in vectors (and lists! (but not sets)) the exact same way:

```clojure

((track {:a [0 1]} {:b [1 0]}) {:a [:zero :one]})
;; => {:b [:one :zero]}

```

`track` also supports arbitrary nesting levels:

### Arbitrary nesting levels

worrying about nesting is a thing of the past:

``` clojure
(def deeptransform
  (track {0 0 1 1 2 2 3 3}
         {:a 0 :b {:c 1 :d {:e 2 :f {:g 3}}}}))

(deeptransform {0 "first" 1 "sec" 2 "therd" 3 "feor"})
;;=> {:a "first", :b {:c "sec", :d {:e "therd", :f {:g "feor"}}}}
```

Next, we do something strange, in an understandable and concise manner:

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


## How it works

The numbers 1 and 2 below are used to dicate *positions* which are leafs in the transformation that will be operated on. 1 and 2 could be any **unique** scalar values like "the thing at a" or ::my-piece-of-data -- any scalar value will work.

``` clojure
(def move-a-key (track {:x 1} {:y 1}))
(move-a-key {:x "MoveMe"})
;;=> {:y "MoveMe"}

```
Let's examine what happens with `move-a-key`. `track` notices the common leaf: `1`. Because the *keypaths* to 1 are [:x] in the first arg and [:y] in the second, `track` returns a function that dissocs the value at [:x] from the input to `move-a-key` and assocs its into path [:y]. Here we give move-a-key the string "MoveMe", but we could just as well have given it any datastructure:

``` clojure
(def move-a-key (track {:x 1} {:y 1}))
(move-a-key {:x [:a :b :c]})
;;=> {:y [:a :b :c]}
```
We see it moved from keypath [:x] to keypath [:y].

### Complex leaf values

`track` greatly simplifies rotating values, too:

Let's simulate a game where there's an active player, and all other players wait in line to become the active one. Once a player has played their turn, they go to the back of the line.

```clojure

;;; Setup the function that moves around players,
;;; no matter what datastructure the players are
;;; represented as:

(def move-players
  (tracks {:active-player 1 :players [2 3 4]}
          {:active-player 2 :players [3 4 1]}))

;;; Here's the datastructure that represents the state of the game.
;;; Notice that the players are more than scalar values!

(defonce game (atom {:active-player {:name "A"}
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

## Using functions with track

Sometimes we want more than a transformation that doesn't update its keys. That's why `track` optionally taks a map containing functions to apply to leafs.

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

## Partial updating values with track

Often we only want to update a subset of a datastructure:

``` clojure

(def transfer-a-to-b (track {:a 1} {:b 1}))

(transfer-a-to-b {:a 1 :c 2})
;;=> {:b 1 :c 2}


(transfer-a-to-b {:a 1 :c 2 :d 3})
;;=> {:b 1 :c 2 :d 3}

```

Note well: key paths not operated on by the function returned by `track` (`[:c]` and `[:d]`) aren't edited.

<<<<<<< HEAD
## Let-track macro for simple destructuring
=======
## track/let macro for simple destructuring
>>>>>>> escherize/master

Destructuring complex nested data structures can be a real pain. Tracks makes this easy.

Much like a regular `let`, symbols in the track pattern will be bound to the value and available the body.

```clojure

<<<<<<< HEAD
(let-track [{:a special-symbol} {:a "Hello World!"}]
  special-symbol)
=======
(t/let [{:a {:b special-symbol}} {:a {:b "Hello World!"}}]
  special-symbol)

>>>>>>> escherize/master
;;=> "Hello World!"

```

<<<<<<< HEAD
=======
track/let also allows you to combine values in a way that `track` does not.

```clojure

(t/let [{:a {:b one} :c {:d two}} {:a {:b 1} :c {:d 2}}]
  (+ one two))

;;=> 3

```

>>>>>>> escherize/master
## Other things

If leaf values don't exist in both the first and second arguments, then they are untouched in the input (like `"??"` below).

``` clojure
(def zed (track {:a [0 1] :z "??"} {:b [1 0]}))

(zed {:a [:zero :one]})
;; => {:b [:one :zero]}

(zed {:a ["ONE" "TWO"] :z "this will be left alone" :c "as will this."})
;;=> {:b ["TWO" "ONE"] :z "this will be left alone" :c "as will this."}

```

## Want more examples?

Check the test namespace!

## License

Copyright © 2016 Bryan Maass

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
