(ns morph.core-test
  (:require [clojure.test :refer :all]
            [morph.core :as morph]
            [clj-time.core :as t]))

(deftest transform-keys-test
  (testing "simple map"
    (let [m {:drink 3 :food 12 :dogs [{:name "biff"} {:name "rover"}]}
          m-prime (morph/transform-keys any? (comp keyword clojure.string/reverse name) m)]
      (is (= {:knird 3 :doof 12 :sgod [{:eman "biff"} {:eman "rover"}]}
             m-prime))))

  (testing "map with mappings"
    (let [m {:drink 3 :food 12 :dogs [{:name "biff"} {:name "rover"}]}
          m-prime (morph/transform-keys
                    any? (comp keyword clojure.string/reverse name) {:knird :fnord} m)]
      (is (= {:fnord 3 :doof 12 :sgod [{:eman "biff"} {:eman "rover"}]}
             m-prime))))

  (testing "simple collection of maps"
    (let [coll [{:drink 3 :food 12 :dogs [{:name "biff"} {:name "rover"}]}
                {:drink 1 :food 4 :dogs [{:name "eric"}]}]
          coll-prime (morph/transform-keys any? (comp keyword clojure.string/reverse name) coll)]
      (is (= [{:knird 3 :doof 12 :sgod [{:eman "biff"} {:eman "rover"}]}
              {:knird 1 :doof 4 :sgod [{:eman "eric"}]}]
             coll-prime))))

  (testing "simple collection of maps with mappings"
    (let [coll [{:drink 3 :food 12 :dogs [{:name "biff"} {:name "rover"}]}
                {:drink 1 :food 4 :dogs [{:name "eric"}]}]
          coll-prime (morph/transform-keys
                       any? (comp keyword clojure.string/reverse name) {:knird :fnord} coll)]
      (is (= [{:fnord 3 :doof 12 :sgod [{:eman "biff"} {:eman "rover"}]}
              {:fnord 1 :doof 4 :sgod [{:eman "eric"}]}]
             coll-prime)))))

(deftest transform-vals-test
  (testing "simple map"
    (let [m {:drink 3 :food 12 :dogs [{:name "biff"} {:name "rover"}]}
          m-prime (morph/transform-vals number? inc m)]
      (is (= {:drink 4 :food 13 :dogs [{:name "biff"} {:name "rover"}]}
             m-prime))))

  (testing "simple collection of maps"
    (let [coll [{:drink 3 :food 12 :dogs [{:name "biff"} {:name "rover"}]}
                {:drink 1 :food 4 :dogs [{:name "eric"}]}]
          coll-prime (morph/transform-vals number? inc coll)]
      (is (= [{:drink 4 :food 13 :dogs [{:name "biff"} {:name "rover"}]}
              {:drink 2 :food 5 :dogs [{:name "eric"}]}]
             coll-prime)))))

(deftest keys->kebab-case-test
  (testing "1-arity"
    (is (= {:foo-bar 1
            :foo-bor 2
            :foo-bur 3}
           (morph/keys->kebab-case {:fooBar  1
                                    :foo_bor 2
                                    :foo-bur 3})))

    (is (= {:foo-bar {:foo-bor 1}}
           (morph/keys->kebab-case {:foo_bar {:fooBor 1}}))))

  (testing "2-arity"
    (is (not= {:phone-number-e164 "not a thing"}
              (morph/keys->kebab-case {:phoneNumberE164 "not a thing"})))

    (is (= {:phone-number-e164 "not a thing"}
           (morph/keys->kebab-case {:phone-number-e-164 :phone-number-e164}
                                   {:phoneNumberE164 "not a thing"})))))

(deftest keys->camelCase-test
  (testing "1-arity"
    (is (= {:fooBar 1
            :fooBor 2
            :fooBur 3}
           (morph/keys->camelCase {:fooBar  1
                                   :foo_bor 2
                                   :foo-bur 3})))

    (is (= {:fooBar {:fooBor 1}}
           (morph/keys->camelCase {:foo_bar {:foo-bor 1}}))))

  (testing "2-arity"
    (is (not= {:x_e10_activationType "not a thing"}
              (morph/keys->camelCase {:x-e10-activation-type "not a thing"})))

    (is (= {:x_e10_activationType "not a thing"}
           (morph/keys->camelCase {:xE10ActivationType :x_e10_activationType}
                                  {:x-e10-activation-type "not a thing"})))))

(deftest recursive-navigators-test
  (let [now      (t/now)
        then     (t/minus (t/now) (t/days 2))
        soon     (t/plus (t/now) (t/days 2))
        deep-map {:foo-bar  "dishwater"
                  :foo-bor  {:some-bors ["junk" "dna" "meh"]}
                  :fooBur   [{:now  now
                              :then then}
                             {:now  now
                              :soon soon}]
                  :moarDeep {:andDeeper {:somehowDeeperStill "doggo"}}}]

    (testing "date transformations"
      (let [result (morph/joda->dates deep-map)]
        ;; dates are java.util.Date now
        (is (instance? java.util.Date (-> result :fooBur first :now)))
        (is (instance? java.util.Date (-> result :fooBur first :then)))
        (is (instance? java.util.Date (-> result :fooBur second :now)))
        (is (instance? java.util.Date (-> result :fooBur second :soon)))
        ;; other things untouched
        (is (= ["junk" "dna" "meh"] (-> result :foo-bor :some-bors)))
        (is (= "dishwater" (-> result :foo-bar)))
        (is (= "doggo" (-> result :moarDeep :andDeeper :somehowDeeperStill)))))

    (testing "string transformations"
      (let [result (morph/transform-vals string? #(str (.substring % 1) (first %) "ay") deep-map)]
        ;; strings are in pig latin now
        (is (= "ishwaterday" (-> result :foo-bar)))
        (is (= ["unkjay" "naday" "ehmay"] (-> result :foo-bor :some-bors)))
        (is (= "oggoday" (-> result :moarDeep :andDeeper :somehowDeeperStill)))
        ;; dates untouched
        (is (= now (-> result :fooBur first :now)))
        (is (= then (-> result :fooBur first :then)))
        (is (= now (-> result :fooBur second :now)))
        (is (= soon (-> result :fooBur second :soon)))))

    (testing "shallow lists"
      (is (every? (partial instance? java.util.Date)
                  (:dates (morph/joda->dates {:dates [now then soon]})))))))
