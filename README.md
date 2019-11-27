# morph

[![Clojars Project](https://img.shields.io/clojars/v/ca.orchard-labs/morph.svg)](https://clojars.org/ca.orchard-labs/morph)
[![CircleCI](https://circleci.com/gh/orchard-labs/morph.svg?style=svg)](https://circleci.com/gh/orchard-labs/morph)

A set of useful transformations, wrapping the excellent [Specter](https://github.com/nathanmarz/specter) library to do some things I need to do a lot, which Specter doesn't trivially support.

## Usage

These transformations operate at different levels, but all make use of a recursive descent into the data structure to apply a transformation throughout to either keys or values in nested structures. In ascending order of abstraction:

These are Specter paths which can be used in regular Specter expressions.

```
user> (doc morph.core/RECURSIVE-MAPS)
-------------------------
morph.core/RECURSIVE-MAPS
  A Specter path for recursively selecting all the maps in a deeply
  nested structure.
nil
user> (doc morph.core/RECURSIVE-MAP-KEYS)
-------------------------
morph.core/RECURSIVE-MAP-KEYS
  A Specter path for recursively selecting all the map keys in a deeply
  nested structure.
nil
user> (doc morph.core/RECURSIVE-MAP-VALS)
-------------------------
morph.core/RECURSIVE-MAP-VALS
  A Specter path for recursively selecting all the map values in a
  deeply nested structure. This matches maps in non-map collections, but not
  values in non-map collections.
nil
```
Higher order recursive descent transformations:

```
user> (doc morph.core/transform-vals)
-------------------------
morph.core/transform-vals
([pred f coll] [f coll])
  Transform values in all nested structures recursively. The `coll`
  argument may be a collection or a map. Values are filtered using the
  `pred` function, which must take a single argument and return
  truthy. If selected, keys are transformed using the `f` function,
  which must take a single argument and return the replacement value.

  Example:

  user> (->> {:str "some string" :vec [1 1 1 1] :top-level-number 8}
             (morph.core/transform-vals number? inc)
             (morph.core/transform-vals string? clojure.string/upper-case))
  {:str "SOME STRING", :vec [2 2 2 2], :top-level-number 9}
nil
user> (doc morph.core/transform-keys)
-------------------------
morph.core/transform-keys
([pred f mappings coll] [pred f coll] [f coll])
  Transform keys in all nested structures recursively. The `coll`
  argument may be a collection or a map. Keys are filtered using the
  `pred` function, which must take a single argument and return
  truthy. If selected, keys are transformed using the `f` function,
  which must take a single argument and return the replacement value.
  A `mappings` map argument may be provided, which will used to rename
  keys once the `f` transformation is complete.

  Examples:

  user> (morph.core/transform-keys
          keyword
          (comp keyword clojure.string/upper-case name)
          {:str "some string" :vec [1 1 1 1] :top-level-number 8})
  {:STR "some string", :VEC [1 1 1 1], :TOP-LEVEL-NUMBER 8}
  user> (morph.core/transform-keys
          keyword
          (comp keyword clojure.string/upper-case name)
          {:STR :charArray}
          {:str "some string" :vec [1 1 1 1] :top-level-number 8})
  {:charArray "some string", :VEC [1 1 1 1], :TOP-LEVEL-NUMBER 8}
  user>
nil
user> 
```
Finally some common tasks, which use the `transform-keys` and `transform-vals` methods to do specifically useful things:

```
user> (doc morph.core/dates->joda)
-------------------------
morph.core/dates->joda
([coll])
  Transform all the java.util.Date objects in an arbitrarily nested
  structure into org.joda.time.DateTime objects.
nil
user> (doc morph.core/joda->dates)
-------------------------
morph.core/joda->dates
([coll])
  Transform all the org.joda.time.DateTime objects in an arbitrarily
  nested structure into java.util.Date objects.
nil
user> 
```
and some which are so blindingly obvious I couldn't think of any documentation which wasn't just the function names:
```clojure
user> (morph.core/keys->snake_case 
        {:dog :cat :thing [1 2 3] :foo {:bar-doof 9 :antEater false :one_snake true :StringBuffer :lol}})
{:dog :cat,
 :thing [1 2 3],
 :foo {:bar_doof 9, :ant_eater false, :one_snake true, :string_buffer :lol}}
user> (morph.core/keys->camelCase 
        {:dog :cat :thing [1 2 3] :foo {:bar-doof 9 :antEater false :one_snake true :StringBuffer :lol}})
{:dog :cat,
 :thing [1 2 3],
 :foo {:barDoof 9, :antEater false, :oneSnake true, :stringBuffer :lol}}
user> (morph.core/keys->PascalCase 
        {:dog :cat :thing [1 2 3] :foo {:bar-doof 9 :antEater false :one_snake true :StringBuffer :lol}})
{:Dog :cat,
 :Thing [1 2 3],
 :Foo {:BarDoof 9, :AntEater false, :OneSnake true, :StringBuffer :lol}}
user> (morph.core/keys->kebab-case 
        {:dog :cat :thing [1 2 3] :foo {:bar-doof 9 :antEater false :one_snake true :StringBuffer :lol}})
{:dog :cat,
 :thing [1 2 3],
 :foo {:bar-doof 9, :ant-eater false, :one-snake true, :string-buffer :lol}}
user> 
```

## License

Copyright © 2018,2019 Jonathan Irving, Paul Lam and others.

Distributed under the Eclipse Public License either version 1.0 or (at your
option) any later version.
