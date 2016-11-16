# tracks

## Example based coding

![Converging Tracks](https://raw.githubusercontent.com/escherize/tracks/master/tracks.jpg)

[![Build Status](https://travis-ci.org/escherize/tracks.svg?branch=master)](https://travis-ci.org/escherize/tracks)

> We become what we behold. We shape our tools, and thereafter our tools shape us.

> ― Marshall McLuhan

## Usage

Add the following line to your leiningen dependencies:

[![Clojars Project](https://img.shields.io/clojars/v/tracks.svg)](https://clojars.org/tracks)

Require tracks in your namespace header:

    (:require [tracks.core :as t :refer [track]])

## Rationale

This is a library dedicated to the concept of *shape*.

    shape n.
        - the external form, contours, or outline of something.
        - the correct or original form or contours of something.
        - an example of something that has a particular form.

    shape v.
        - to give definite form, organization, or character to.
        - fashion or form.

It's common to grapple with large maps whose shapes are uncomfortable to reason about.

`tracks` simplifies transformations and destructuring of Clojure datastructures. Instead of describing how to do a transformation, tracks allows the user to create those transformations by __example__.  This makes writing complex code that takes one shape and transforms them to another *dead simple*.

## Examples

### track/let

Destructuring complex nested data structures can be a real pain. Tracks makes this easy. Much like `clojure.core/let`, symbols in the track pattern will be bound to the value and available the body. Unlike `clojure.core/let` we supply a binding form of *the same shape* as the data we are interested in.

```clojure

(t/let [{:a {:b [greeting person]}} ;;<- binding form
        {:a {:b ["Hello" "World"]}} ;;<- data we want to get at
        ]
  (str greeting " " person "!"))

;;=> "Hello World!"

(t/let [{:a {:b x} :c {:d y}}
        {:a {:b 1} :c {:d 2}}]
  (+ x y))

;;=> 3

```
### track/track for building functions

`track` returns a function which takes data of the shape of its first argument.

Below, the function returned by `track` will take a map with keys `:a` and `:b` and move the value at `:a` to `:b`, and the value at `:b` to `:a`:

``` clojure
(track {:a one :b two}
       {:a two :b one})

;;=> anonymous fn

(def swap-a-b (track {:a one :b two}
              {:a two :b one}))
(swap-a-b {:a 100 :b 3000})

;;=> {:a 3000 :b 100}
```

`deftrack` does the same thing, but binds it too:

``` clojure
(deftrack swap-a-b {:a one :b two} {:a two :b one})
(swap-a-b {:a 100 :b 3000})

;;=> {:a 3000 :b 100}
```

We can move positions in vectors and deeply nested maps in exactly the same way:

```clojure
((track {:a [zero one]}
        {:b [one zero]})
  {:a [:zero :one]})

;; => {:b [:one :zero]}
```

### Arbitrary nesting levels

Deep thinking about deeply nested shapes is a bygone era:

``` clojure
(deftrack deeptx
  {0 zero, 1 one, 2 two, 3 three} ;; <- deeptx takes a map with this shape
  {:a zero :b {:c one :d {:e two :f {:g three}}}} ;; <- deeptx then returns one with this shape
  )

(deeptx {0 "first" 1 "second" 2 "third" 3 "fourth"})
;;=> {:a "first", :b {:c "second", :d {:e "third", :f {:g "fourth"}}}}
```
### Complex leaf values

`track` greatly simplifies rotating values, too:

Let's simulate a game where there's an active player, and all other players wait in line to become the active one. Once a player has played their turn, they go to the back of the line.

```clojure

;;; Setup the function that moves around players,
;;; no matter what datastructure the players are
;;; represented as:

(deftrack move-players
  {:active-player p1 :players [p2 p3 p4]}
  {:active-player p2 :players [p3 p4 p1]})

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

### Multiple endpoints

Like a train track, sometimes one track can split into many. With `track` the values can be duplicated.

``` clojure
(deftrack one-to-many x {:a x :b {:c [x x]}})

(one-to-many "?")

;;=> {:a "?", :b {:c ["?" "?"]}}
```
## How it works

`track` is implemented in terms of `let`

``` clojure
(def move-a-key (track {:x one} {:y one}))

(move-a-key {:x "MoveMe"})

;;=> {:y "MoveMe"}

(move-a-key {:x [:a :b :c]})

;;=> {:y [:a :b :c]}
```

We see it moves any value from keypath [:x] to keypath [:y].

The way it does it is by moving `{:x one}` into a `let` like so:

``` clojure
          ;; vvvvvvvv---- this is the first arg to track
(tracks/let [{:x one} input]
    ;; so now one is bound to (get input :x)
 ;;  vvvvvv---- this is the 2nd arg to track
    {:y one})
```

## Want more examples?

Check the test namespace!

## License

Copyright © 2016 Bryan Maass

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.