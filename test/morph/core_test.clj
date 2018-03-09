(ns morph.core-test
  (:require [clojure.test :refer :all]
            [morph.core :as morph]
            [clj-time.core :as t]))

(deftest keys->kebab-case-test
  (is (= {:foo-bar 1
          :foo-bor 2
          :foo-bur 3}
         (morph/keys->kebab-case {:fooBar  1
                                  :foo_bor 2
                                  :foo-bur 3})))

  (is (= {:foo-bar {:foo-bor 1}}
         (morph/keys->kebab-case {:foo_bar {:fooBor 1}}))))

(deftest keys->camelCase-test
  (is (= {:fooBar 1
          :fooBor 2
          :fooBur 3}
         (morph/keys->camelCase {:fooBar  1
                                 :foo_bor 2
                                 :foo-bur 3})))

  (is (= {:fooBar {:fooBor 1}}
         (morph/keys->camelCase {:foo_bar {:foo-bor 1}}))))

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
