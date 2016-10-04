(ns tracks.core
  (:require [clojure.walk :as w]
            [clojure.set :as set]))

(defn keys-into [m]
  (if (map? m)
    (vec
     (mapcat (fn [[k v]]
               (let [nested (->> (keys-into v)
                                 (filter (comp not empty?))
                                 (map #(into [k] %)))]
                 (if (seq nested) nested [[k]])))
             m))
    []))


(defn- meta-print [& xs]
  (doseq [x xs]
    (print x)
    (when (meta x) (println (str " | With meta: " (meta x))))
    (println)))

(defn m-able? [x]
  (and (not= (type x) clojure.lang.MapEntry)
       (coll? x)
       (or (list? x) (vector? x))))

(defn ->m-shallow [coll]
  (let [m-map (if (or (list? coll) (vector? coll))
                (->> coll
                     (map-indexed (fn [idx x] [idx x]))
                     (into {}))
                coll)]
    (-> m-map
        (with-meta {:type (type coll)}))))

(defn ->m [coll]
  (w/prewalk
   (fn [x] (if (m-able? x) (->m-shallow x) x))
   coll))

(defn <-m-shallow [map-with-meta]
  (if-let [{:keys [type]} (meta map-with-meta)]
    (let [unmap (fn [mwm] (map mwm (range (count mwm))))]
      (condp #(%1 %2) type
        #{clojure.lang.PersistentArrayMap}        map-with-meta
        #{clojure.lang.PersistentList
          clojure.lang.PersistentList$EmptyList}  (unmap map-with-meta)
        ;; TODO: sets?
        ;; #{clojure.lang.PersistentHashSet}         (set (unmap map-with-meta))
        #{clojure.lang.PersistentVector}          (vec (unmap map-with-meta))
        :unknown-type (throw (Exception. (str "<-m-shallow recieved unknown type:" (meta map-with-meta))))))
    (throw (Exception. (str "<-m-shallow recieved no type for :" map-with-meta)))))

(defn <-m [coll]
  (w/prewalk
   (fn [x] (if (:type (meta x))
             (<-m-shallow x) x))
   coll))

(defn paths [m]
  (->> m
       keys-into
       (mapv (fn [path] [(get-in m path) path]))
       (into {})))

(defn transform
  "Given two maps in and out, returns f s.t. f(in) = out for all shared keys."
  [in out]
  (let [in-paths (paths in)
        out-paths (paths out)]
    (fn [in-map]
      (loop [out-m {}
             cvs (vec (set/intersection
                       (set (keys in-paths))
                       (set (keys out-paths))))]
        (let [in-path (get in-paths (first cvs))
              out-path (get out-paths (first cvs))]
          (if (nil? cvs)
            out-m
            (recur
             (assoc-in out-m out-path (get-in in-map in-path))
             (next cvs))))))))

(comment

  (meta (->m []))

  (def swap-a-b (transform {:a 1} {:b 1}))

  (swap-a-b {:a 100 :b 3000})

  (swap! (atom {0 "ayee" 1 "b" 2 "2000" 3 "321931"})
         (transform {0 0 1 1 2 2 3 3}
                    {:a 0 :b {:c 1 :d {:e 2 :f {:g 3}}}}))

  (def deep-ways (transform {0 0 1 1 2 2 3 3}
                            {:a 0 :b {:c 1 :d {:e 2 :f {:g 3}}}}))

  (deep-ways {0 "first" 1 "sec" 2 "thred" 3 "fer"})

  ;; => {:item {:a "BBB", :b "HHH", :c "SSS"}}


  (transform {0 0 1 1 2 2 3 3}
             {:a 0 :b {:c 1 :d {:e 2 :f {:g 3}}}})

  )
