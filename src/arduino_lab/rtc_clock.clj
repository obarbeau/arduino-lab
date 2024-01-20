(ns arduino-lab.rtc-clock
  "╭───────────────────╮
   │ RTC clock DS 1302 │
   ╰───────────────────╯"
  (:require [clodiuno.core :as ccore :refer [LOW HIGH INPUT OUTPUT]]
            [clodiuno-debug.core :as core]
            [clodiuno-debug.utils :as utils]))

(def CLK 38)
(def IO  39)
(def CE  40)

(def pin-mapping [{:num CLK :color :white  :name "CLK"}
                  {:num IO  :color :yellow :name "IO"}
                  {:num CE  :color :magenta :name "CE/_RST"}])

(defn init-rtc-clock
  "Init the pins for DS1302"
  [board]
  (ccore/pin-mode board CLK OUTPUT)
  (ccore/digital-write board CLK LOW)
  ;; is both input & output, starts with output and write LOW
  (ccore/pin-mode board IO OUTPUT)
  (ccore/digital-write board IO LOW)
  (ccore/pin-mode board CE OUTPUT)
  (ccore/digital-write board CE HIGH))

;; bit:       0  1  2  3  4  5    6  7
;; io:     R/_W A0 A1 A2 A3 A4 R/_C  1
(defn command-byte [read? address calendar?]
  (cond-> 2r10000000
    (not calendar?) (bit-set 6)
    read? (bit-set 0)
    true (bit-or (bit-shift-left address 1))))

(defn command-for-read-calendar [address]
  (command-byte true address true))

(defn command-for-read-ram [address]
  (command-byte true address false))

(defn command-for-write-calendar [address]
  (command-byte false address true))

(defn command-for-write-ram [address]
  (command-byte false address false))

(defn shift-rtc-clock [board & {:keys [how-much wait]
                                :or {how-much 8 wait 100}}]
  (doseq [_ (range 0 how-much)]
    (core/impulse board CLK :wait wait)))

(defn write-rtc-clock [board command]
  (doseq [x (range 0 8)]
    (ccore/pin-mode board IO OUTPUT)
    (ccore/digital-write board
                         IO
                         (if (bit-test (bit-shift-left command x) 7) HIGH LOW))
    (shift-rtc-clock board :how-much 1)))

;; TODO the reading must be done on the falling edge of the clock!
(defn read-byte [board pin]
  (let [result (atom 2r0)]
    (doseq [x (range 0 8)]
      (when (= HIGH (ccore/digital-read board pin))
        (println "high!")
        (swap! result #(bit-set % x)))
      (shift-rtc-clock board :how-much 1))
    @result))

;; what = 0 = seconds
(defn read-calendar [board what]
  (write-rtc-clock board (command-for-read-calendar what))
  (ccore/pin-mode board IO INPUT)
  (read-byte board IO))

(comment

  (import '(gnu.io CommPortIdentifier))
  (CommPortIdentifier/getPortIdentifiers)

  (utils/list-ports)
  ;; connect board
  (def board (core/connect :pin-mapping pin-mapping
                           :debug true
                           :output-name "rtc"))
  (clojure.pprint/pprint board)
  (clojure.pprint/pprint (:pin-mapping @board))

  (init-rtc-clock board)

  (core/impulse board CLK :wait 0)

  (read-calendar board 0)

  (utils/export-signal board)

  (core/close-board board)
  ;;
  )