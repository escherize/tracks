(ns tracks.core
  (:refer-clojure :exclude [let]))

(defn- symbol-paths [x]
  (letfn [(f [x p]
            (cond
              (symbol? x) {x p}
              (map? x) (apply merge (map #(f (val %) (conj p (key %))) x))
              (sequential? x) (apply merge (map-indexed #(f %2 (conj p %1)) x))))]
    (f x [])))

(defn path->value [p x]
  (if (empty? p)
    x
    (recur (next p)
           (if (sequential? x)
             (nth x (first p))
             (get x (first p))))))

(defmacro assert-args
  [& pairs]
  `(do (when-not ~(first pairs)
         (throw (IllegalArgumentException.
                 (str (first ~'&form) " requires " ~(second pairs) " in " ~'*ns* ":" (:line (meta ~'&form))))))
       ~(clojure.core/let [more (nnext pairs)]
          (when more
            (list* `assert-args more)))))

(defmacro let [bindings & body]
  (assert-args
   (vector? bindings) "a vector for its binding"
   (even? (count bindings)) "an even number of forms in binding vector")
  (let* [val-syms (repeatedly gensym)
         paths (->> bindings
                    (take-nth 2)
                    (map (fn [sym struct]
                           (->> struct
                                symbol-paths
                                (map (fn [[k v]]
                                       [k [sym v]]))
                                (into {})))
                         val-syms)
                    (apply merge))]
    `(let* [~@(mapcat vector val-syms (take-nth 2 (next bindings)))
            ~@(mapcat (fn [[k [sym v]]]
                        [k `(path->value ~v ~sym)])
                      paths)]
       ~@body)))

(defmacro track [in & outs]
  `(fn [in#]
     (let [~in in#]
       ~@outs)))

(defmacro deftrack [name in & outs]
  `(def ~name (track ~in ~@outs)))
