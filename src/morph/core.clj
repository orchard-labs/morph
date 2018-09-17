(ns morph.core
  (:require [camel-snake-kebab.core :as csk]
            [com.rpl.specter :as specter]
            [clj-time.coerce :as coerce]
            [taoensso.truss :refer [have]])
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

(defn trap-mappings
  [mappings]
  #(get mappings % %))

(defn transform-keys
  ([pred f mappings m]
   (transform-keys pred (comp (trap-mappings mappings) f) m))
  ([pred f m]
   (specter/transform [RECURSIVE-MAP-KEYS pred] f (have map? m)))
  ([f m]
   (transform-keys (constantly true) f m)))

(defn transform-vals
  ([pred f m]
   (specter/transform [LEAF-NODES pred] f (have map? m)))
  ([f m]
   (transform-vals (constantly true) f m)))

(defn dates->joda
  "Transform all the java.util.Date objects in an arbitrarily nested
  structure into org.joda.time.DateTime objects."
  [m]
  (transform-vals (partial instance? Date) coerce/to-date-time m))

(defn joda->dates
  "Transform all the org.joda.time.DateTime objects in an arbitrarily
  nested structure into java.util.Date objects."
  [m]
  (transform-vals (partial instance? DateTime) coerce/to-date m))

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
