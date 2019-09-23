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

This is a library to handle _shapes_. What's a shape?

    shape n.
        - the correct or original form or contours of something.
        - an example of something that has a particular form.

    shape v.
        - to give definite form, organization, or character to.

It's common to grapple with deeply nested arguments whose shapes are difficult to know without running the code. The data we love, tho pure and immutable can be nested and complex. This approach removes the cognitive burden needed to understand our datastructures.

## Examples

#### deftrack example:

Instead of describing _how to do a transformation_, tracks allows the user to _create those transformations declaratively_.  This makes writing code that takes one shape and transforms them to another *dead simple*.

Let's consider this data as our input. Typically this shape needs to be 'reverse-engineered' by reading and understanding code with `get-in`, destructuring, and other such operations.

``` clojure
(def buyer-information-map
  {:buyer-info
   {:is-guest true
    :primary-contact {:name {:first-name "Bob" :last-name "Ross"}
                      :phone {:complete-number "123123123"}
                      :email {:email-address "thebobguy@rossinator.com"}}}})
```


Next, Let's create a function that takes this particular _shape_ and returns another representing a notification for a customer.


```clojure
(require '[tracks.core :as t :refer [deftrack]])

(deftrack notify-buyer
  {:buyer-info {:is-guest guest?                                    ;; 1
                :primary-contact {:name {:first-name firstname
                                         :last-name lastname}
                                  :phone {:complete-number phone}
                                  :email {:email-address email}}}}
  (when guest?                                                      ;; 2
    {:command :send-notification
     :address email
     :phone phone
     :text (str "Hi, " firstname " " lastname)}))
;; => #function[user/notify-buyer]

(notify-buyer buyer-information-map)
;; => {:command :send-notification
;;     :address "thebobguy@rossinator.com"
;;     :phone "123123123"
;;     :text "Hi, Bob Ross"}
```

1. `deftrack` expects data of this shape
2. `deftrack` returns this value

### What is going on here?

For every symbol in the binding form to `deftrack` (1 above), `deftrack` generates a program to seamlessly write the get / get-in / assoc-in / assoc / etc. sort of accessing code and allows you to focus on __your data__.

## Destructuring

You may be thinking to yourself: Clojure already has destructuring! That's true, let's compare using `deftrack` against `defn` style destructuring:

``` clojure
(deftrack notify-buyer
  {:buyer-info {:is-guest guest?
                :primary-contact {:name {:first-name firstname
                                         :last-name lastname}
                                  :phone {:complete-number phone}
                                  :email {:email-address email}}}}
  (when guest?
    {:command :send-notification
     :address email
     :phone phone
     :text (str "Hi, " firstname " " lastname)}))

(defn notify-buyer-2 [{{guest? :is-guest,
                        {{firstname :first-name, lastname :last-name} :name,
                         {phone :complete-number} :phone,
                         {email :email-address} :email}
                        :primary-contact}
                       :buyer-info}]
  (when guest?
    {:command :send-notification
     :address email
     :phone phone
     :text (str "Hi, " firstname " " lastname)}))
```

I think you'd agree which of those is easier to read.

### deftrack metadata

deftrack plays nice with arglists metadata, enabling your editor to explain what sort of shape a function created with `deftrack` takes.

``` clojure
(deftrack move-some-keys
  {:a a :b b :c c :d {:e e}}
  {:a b :b c :c e :d {:e a}})

(move-some-keys {:a 1 :b 2 :c 3 :d {:e 4}})
;; => {:a 2, :b 3, :c 4, :d {:e 1}}

(:arglists (meta #'move-some-keys))
;; => ([{a :a, b :b, c :c, {e :e} :d}])
```

Since we don't like to read deeply destructured arglists, `deftracks` also goes one step further, and includes what shape your function _expects_. (Todo: make this work with editors).

``` clojure
(:tracks/expects (meta #'move-some-keys))
;; => {:a a, :b b, :c c, :d {:e e}}
```

#### let example

For more flexible flowing of data, here's __tracks/let__, which allows for the same data-oriented style but with multiple arguments, etc.

``` clojure
(require '[tracks.core :as t :refer [deftrack]])

;; Please Notice: you usually don't get to see what some-data looks like! :)
(def some-data
  {:more-info {:price-for-this-order 10}
   :order-info {:amount-bought-from-my-company 3}})

;;in another part of your program:


;; you can use t/let:
(t/let [{:more-info {:price-for-this-order price}
         :order-info {:amount-bought-from-my-company quantity}} some-data]
  (* price quantity))
;;=> 30

;; or you can use deftrack:
(deftrack calculate-price-for-order
  {:more-info {:price-for-this-order price}
   :order-info {:amount-bought-from-my-company quantity}}
  (* price quantity))

(calculate-price-for-order some-data)
;;=> 30
```

### Arbitrary nesting levels

Deep contemplation about deeply nested shapes is the old way.

``` clojure
(deftrack deeptx
  {0 zero
   1 one
   2 two
   3 three} ;; <- deeptx takes a map with this shape
  {:a zero
   :b {:c one
       :d {:e two
           :f {:g three}}}} ;; <- deeptx then returns one with this shape
  )

(deeptx {0 "first" 1 "second" 2 "third" 3 "fourth"})
;;=> {:a "first", :b {:c "second", :d {:e "third", :f {:g "fourth"}}}}
```

### Complex leaf values

Let's simulate a game where there's an active player, and all other players wait in a queue to become the active one. Once a player has played their turn, they naturally go to the back of the queue.

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
(deftrack one-to-many {:clone-me x} {:a x :b {:c [x x]}})

(one-to-many {:clone-me "?"})

;;=> {:a "?", :b {:c ["?" "?"]}}
```

## Want more examples?

Check the test namespace!
