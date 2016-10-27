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
  (let* [bindings'
         (->> bindings
              (partition 2)
              (mapcat (fn [[shape v]]
                        (->> shape
                             symbol-paths
                             (mapcat (fn [[k p]]
                                       [k (path->value p v)])))))
              vec)]
    `(let* ~bindings'
       ~body)))
