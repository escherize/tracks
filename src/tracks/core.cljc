(ns tracks.core)

(defn symbol-paths [x]
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

(defmacro let [bindings body]
  {:pre [(vector? bindings)
         (even? (count bindings))]}
  (let* [val-syms (repeatedly (/ (count bindings) 2) gensym)
         paths (->> bindings
                    (partition 2)
                    (map (fn [sym [struct _]]
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
       ~body)))
