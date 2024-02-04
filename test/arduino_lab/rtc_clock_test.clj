(ns arduino-lab.rtc-clock-test
  (:require [arduino-lab.rtc-clock :as sut]
            [clodiuno-debug.core :as debug-core]
            [clojure.test :refer [deftest is]]))

(deftest command-byte-test
  (is (= 2r10000001
         (sut/command-byte true 0 true)))
  (is (= 2r10000000
         (sut/command-byte false 0 true)))
  (is (= 2r11000000
         (sut/command-byte false 0 false)))
  (is (= 2r11111110
         (sut/command-byte false 31 false)))
  (is (= 2r11101010
         (sut/command-byte false 21 false))))

(deftest command-for-read-calendar-test
  (is (= 2r10000001
         (sut/command-for-read-calendar 0)))
  (is (= 2r10000011
         (sut/command-for-read-calendar 1)))
  (is (= 2r10000101
         (sut/command-for-read-calendar 2)))
  (is (= 2r10010001
         (sut/command-for-read-calendar 8))))

(deftest command-for-read-ram-test
  (is (= 2r11000001
         (sut/command-for-read-ram 0)))
  (is (= 2r11000011
         (sut/command-for-read-ram 1)))
  (is (= 2r11000101
         (sut/command-for-read-ram 2)))
  (is (= 2r11010001
         (sut/command-for-read-ram 8))))

(deftest command-for-write-calendar-test
  (is (= 2r10000000
         (sut/command-for-write-calendar 0)))
  (is (= 2r10000010
         (sut/command-for-write-calendar 1)))
  (is (= 2r10000100
         (sut/command-for-write-calendar 2)))
  (is (= 2r10010000
         (sut/command-for-write-calendar 8))))

(deftest command-for-write-ram-test
  (is (= 2r11000000
         (sut/command-for-write-ram 0)))
  (is (= 2r11000010
         (sut/command-for-write-ram 1)))
  (is (= 2r11000100
         (sut/command-for-write-ram 2)))
  (is (= 2r11010000
         (sut/command-for-write-ram 8))))

(deftest shift-rtc-clock-test
  (let [board (debug-core/connect :pin-mapping sut/pin-mapping
                                  :debug true
                                  :output-name "shift-rtc-clock-test")]
    (sut/shift-rtc-clock board)
    (sut/shift-rtc-clock board :how-much 3)
    (is (= (slurp "test-resources/shift-rtc-clock-test.md")
           (slurp "output/shift-rtc-clock-test.md")))))