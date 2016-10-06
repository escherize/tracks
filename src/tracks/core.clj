(ns tracks.core
  (:require [clojure.walk :as w]
            [clojure.set :as set]))

(defn- keys-into [m]
  (if (map? m)
    (vec
     (mapcat (fn [[k v]]
               (let [nested (->> (keys-into v)
                                 (filter (comp not empty?))
                                 (map #(into [k] %)))]
                 (if (seq nested) nested [[k]])))
             m))
    []))

(defn- ->m-shallow [coll]
  (if (or (list? coll) (vector? coll))
    (->> coll
         (map-indexed (fn [idx x] [idx x]))
         (into {}))
    coll))

(defn- m-able? [x]
  (and (not= (type x) clojure.lang.MapEntry)
       (coll? x)
       (or (list? x) (vector? x))))

(defn ->m [coll]
  (w/prewalk
   (fn [x] (if (m-able? x) (->m-shallow x) x))
   coll))

(defn paths [m]
  (let [m-map (->m m)
        keys-into-m (keys-into m-map)]
    (->> keys-into-m
         (mapv (fn [path] [(get-in m path) path]))
         (into {}))))

(defn track
  "Given two maps in and out, returns f s.t. f(in) = out for all shared keys."
  [in out]
  (let [in-paths (paths in)
        out-paths (paths out)]
    (fn [input]
      (loop [out-map out
             common-vals (vec
                          (set/intersection
                           (set (keys in-paths))
                           (set (keys out-paths))))]
        (let [in-path (get in-paths (first common-vals))
              out-path (get out-paths (first common-vals))]
          (if (nil? common-vals)
            out-map
            (recur
             (assoc-in out-map out-path (get-in input in-path))
             (next common-vals))))))))
