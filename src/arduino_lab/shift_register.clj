(ns arduino-lab.shift-register
  "╭────────────────────────╮
   │ shift-register 74HC595 │
   ╰────────────────────────╯"
  (:require [clodiuno.core :as ccore :refer [LOW HIGH OUTPUT]]
            [clodiuno-debug.core :as core]
            [clodiuno-debug.utils :as utils]))

(def pin-mapping [{:num 48 :color :yellow :name "data"}
                  ;; shift-register register clock
                  {:num 49 :color :white  :name "SH-CP"}
                  ;; storage clock
                  {:num 50 :color :green  :name "ST-CP"}
                  {:num 51 :color :blue   :name "_MR"}
                  {:num 52 :color :black  :name "_OE"}])

(def DATA  48)
(def SH-CP 49)
(def ST-CP 50)
(def _MR   51)
(def _OE   52)

(defn init-shift-register-register
  "init pins for 74HC595"
  [board]
  (ccore/pin-mode board DATA OUTPUT)
  (ccore/pin-mode board SH-CP OUTPUT)
  (ccore/pin-mode board ST-CP OUTPUT)
  (ccore/pin-mode board _MR OUTPUT)
  (ccore/pin-mode board _OE OUTPUT)
  (ccore/digital-write board SH-CP LOW)
  (ccore/digital-write board ST-CP LOW))

(defn clear_register [board]
  (ccore/digital-write board _MR LOW)
  (ccore/digital-write board _OE LOW)
  (core/impulse board ST-CP)
  (ccore/digital-write board _MR HIGH))

(defn shift-register [board & {:keys [how-much wait]
                               :or {how-much 8 wait 100}}]
  (doseq [_ (range 0 how-much)]
    (core/impulse board SH-CP :wait wait)
    (core/impulse board ST-CP :wait wait)))

(defn sens1
  "One led only, shifting left"
  [board speed]
  (clear_register board)
  (ccore/digital-write board DATA HIGH)
  (shift-register board :wait speed :how-much 1)
  (ccore/digital-write board DATA LOW)
  (shift-register board :wait speed :how-much 8))

(defn sens2
  "One led only, shifting right"
  [board]
  (doseq [led (range 7 -1 -1)]
    (clear_register board)
    (ccore/digital-write board _OE HIGH)
    (ccore/digital-write board DATA HIGH)
    (shift-register board :wait 0 :how-much 1)
    (ccore/digital-write board DATA LOW)
    (shift-register board :wait 0 :how-much led)
    (ccore/digital-write board _OE LOW)
    (Thread/sleep 50)))

(defn register-load [board i]
  (doseq [x (range 0 8)]
    (ccore/digital-write board
                         DATA
                         (if (bit-test (bit-shift-left i x) 7) HIGH LOW))
    (shift-register board :how-much 1)))

(defn vagues [board]
  (doseq [speed [20 10 5 2]
          value [HIGH LOW]]
    (ccore/digital-write board DATA value)
    (shift-register board :wait speed)))

(comment
  (utils/list-ports)
  (def board (core/connect pin-mapping :debug false))

  (clojure.pprint/pprint board)

  ;; just integrated led test
  (core/integrated-led-blink board)

  ;; init
  (init-shift-register-register board)

  ;; clear register
  (clear_register board)

  (register-load board 2r10101010)
  (register-load board 2r11110000)
  (register-load board 2r00001111)
  (register-load board 255)

  (vagues board)

  (doseq [speed [20 10]]
    (sens1 board speed))

  (sens1 board 1000)

  (do
    (sens1 board 50)
    (sens2 board))

  (ccore/digital-write board DATA HIGH)
  (ccore/digital-write board DATA LOW)
  (shift-register board :how-much 8)

  (core/close-board board)
  ;;
  )