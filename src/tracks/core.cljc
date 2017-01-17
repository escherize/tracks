(ns tracks.core
  (:refer-clojure :exclude [let]))

(alias 'cc 'clojure.core)

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

(defmacro assert-args
  [& pairs]
  `(do (when-not ~(first pairs)
         (throw (IllegalArgumentException.
                 (str (first ~'&form) " requires " ~(second pairs) " in " ~'*ns* ":" (:line (meta ~'&form))))))
       ~(cc/let [more# (nnext pairs)]
          (when more#
            (list* `assert-args more#)))))

(defmacro let [bindings & body]
  (assert-args
   (vector? bindings) "a vector for its binding"
   (even? (count bindings)) "an even number of forms in binding vector")
  (let* [paths (->> bindings
                    (partition 2)
                    (map (fn [[tpatt input]]
                           [(gensym) tpatt input]))
                    (mapcat
                     (fn [[sym tpatt input]]
                       (->> tpatt
                            symbol-paths
                            (mapcat
                             (fn [[k v]]
                               [k `(path->value ~v ~sym)]))
                            (concat [sym input]))))
                    (vec))]
    `(let* ~paths ~@body)))

(defn dissoc-in
  "Dissociates an entry from a nested associative structure returning a new
  nested structure. keys is a sequence of keys. Any empty maps that result
  will not be present in the new structure."
  [m [k & ks :as keys]]
  (if ks
    (if-let [nextmap (get m k)]
      (cc/let [newmap# (dissoc-in nextmap ks)]
        (if (seq newmap#)
          (assoc m k newmap#)
          (dissoc m k)))
      m)
    (dissoc m k)))

(defn dissoc-all-in [m paths]
  (reduce (fn [m path] (dissoc-in m path)) m paths))

(defmacro clean-keys [in out]
  `(dissoc-all-in '~out (vals (symbol-paths '~in))))

(defmacro track [in & outs]
  `(fn [x#]
     (let [~in x#] ~@outs)))

(defmacro deftrack [name in & outs]
  `(def ~name (track ~in ~@outs)))
