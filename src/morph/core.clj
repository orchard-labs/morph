(ns morph.core
  (:require [camel-snake-kebab.core :as csk]
            [com.rpl.specter :as specter]
            [clj-time.coerce :as coerce])
  (:import [org.joda.time DateTime]
           [java.util Date]))

(def
  ^{:doc "A Specter path for recursively selecting all the maps in a deeply
  nested structure."}
  RECURSIVE-MAPS
  (specter/recursive-path
    [] p (specter/cond-path
           map? (specter/continue-then-stay specter/MAP-VALS p)
           coll? [specter/ALL p])))

(def
  ^{:doc "A Specter path for recursively selecting all the map keys in a deeply
  nested structure."}
  RECURSIVE-MAP-KEYS
  (specter/path RECURSIVE-MAPS specter/MAP-KEYS))

(def
  ^{:doc "A Specter path for recursively selecting all the map values in a
  deeply nested structure. This matches maps in non-map collections, but not
  values in non-map collections."}
  RECURSIVE-MAP-VALS
  (specter/path RECURSIVE-MAPS specter/MAP-VALS))

(def
  ^{:doc "A Specter path for recursively selecting all the map values in a
  deeply nested structure. Note that this also matches values within non-map
  collections."}
  LEAF-NODES
  (let [non-map-collection (specter/pred #(and (not (map? %)) (coll? %)))]
    (specter/multi-path RECURSIVE-MAP-VALS
                        (specter/path RECURSIVE-MAP-VALS
                                      non-map-collection
                                      specter/ALL))))

(defmulti transform-keys
  "Transform keys in all nested structures recursively. The `coll`
  argument may be a collection or a map. Keys are filtered using the
  `pred` function, which must take a single argument and return
  truthy. If selected, keys are transformed using the `f` function,
  which must take a single argument and return the replacement value.
  A `mappings` map argument may be provided, which will used to rename
  keys once the `f` transformation is complete.

  Examples:

  user> (morph.core/transform-keys
          keyword?
          (comp keyword clojure.string/upper-case name)
          {:str \"some string\" \"strKey\" 99 :vec [1 1 1 1] :top-level-number 8})
  {:STR \"some string\", \"strKey\" 99, :VEC [1 1 1 1], :TOP-LEVEL-NUMBER 8}
  user> (morph.core/transform-keys
          keyword?
          (comp keyword clojure.string/upper-case name)
          {:STR :charArray}
          {:str \"some string\" \"strKey\" 99 :vec [1 1 1 1] :top-level-number 8})
  {:charArray \"some string\", \"strKey\" 99, :VEC [1 1 1 1], :TOP-LEVEL-NUMBER 8}
  user>"
  {:arglists '([pred f mappings coll] [pred f coll] [f coll])}
  (fn [& args] (-> args last type)))
(defmethod transform-keys clojure.lang.IPersistentMap
  ([pred f mappings m]
   (transform-keys pred (comp #(mappings % %) f) m))
  ([pred f m]
   (specter/transform [RECURSIVE-MAP-KEYS pred] f m))
  ([f m]
   (transform-keys (constantly true) f m)))

(defmethod transform-keys clojure.lang.IPersistentCollection
  ([pred f mappings coll]
   (transform-keys pred (comp #(mappings % %) f) coll))
  ([pred f coll]
   (map #(transform-keys pred f %) coll))
  ([f coll]
   (transform-keys (constantly true) f coll)))

(defmulti transform-vals
  "Transform values in all nested structures recursively. The `coll`
  argument may be a collection or a map. Values are filtered using the
  `pred` function, which must take a single argument and return
  truthy. If selected, keys are transformed using the `f` function,
  which must take a single argument and return the replacement value.

  Example:

  user> (->> {:str \"some string\" :vec [1 1 1 1] :top-level-number 8}
             (morph.core/transform-vals number? inc)
             (morph.core/transform-vals string? clojure.string/upper-case))
  {:str \"SOME STRING\", :vec [2 2 2 2], :top-level-number 9}"
  {:arglists '([pred f coll] [f coll])}
  (fn [& args] (-> args last type)))


(defmethod transform-vals clojure.lang.IPersistentMap
  ([pred f m]
   (specter/transform [LEAF-NODES pred] f m))
  ([f m]
   (transform-vals (constantly true) f m)))

(defmethod transform-vals clojure.lang.IPersistentCollection
  ([pred f coll]
   (map #(transform-vals pred f %) coll))
  ([f coll]
   (transform-vals (constantly true) f coll)))

(defn dates->joda
  "Transform all the java.util.Date objects in an arbitrarily nested
  structure into org.joda.time.DateTime objects."
  [coll]
  (transform-vals (partial instance? Date) coerce/to-date-time coll))

(defn joda->dates
  "Transform all the org.joda.time.DateTime objects in an arbitrarily
  nested structure into java.util.Date objects."
  [coll]
  (transform-vals (partial instance? DateTime) coerce/to-date coll))

(def keys->kebab-case
  (partial transform-keys
           #(or (keyword? %) (string? %))
           csk/->kebab-case))

(def keys->snake_case
  (partial transform-keys
           #(or (keyword? %) (string? %))
           csk/->snake_case))

(def keys->camelCase
  (partial transform-keys
           #(or (keyword? %) (string? %))
           csk/->camelCase))

(def keys->PascalCase
  (partial transform-keys
           #(or (keyword? %) (string? %))
           csk/->PascalCase))
