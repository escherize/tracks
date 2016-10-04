# tracks

This library is intended to simplify transformations of Clojure datastructures. Instead of saying what we want done to them in a sequential order, track lets you create functions by __example__.

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


It works by finding paths to identitical values in the inputs and automatically producing a function that will make those changes.


## Caveats

Since sets are not ordered, this library doesn't support transformations on them. Also, using track with lists is currently unsupported.

## License

Copyright © 2016 Bryan Maass

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
