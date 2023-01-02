(ns arduino-lab.shift-register
  "╭────────────────────────╮
   │ shift-register 74HC595 │
   ╰────────────────────────╯"
  (:require [arduino-lab.core :as acc]
            [clodiuno.core :as core
             :refer [LOW HIGH OUTPUT]]
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
  (core/pin-mode board DATA OUTPUT)
  (core/pin-mode board SH-CP OUTPUT)
  (core/pin-mode board ST-CP OUTPUT)
  (core/pin-mode board _MR OUTPUT)
  (core/pin-mode board _OE OUTPUT)
  (core/digital-write board SH-CP LOW)
  (core/digital-write board ST-CP LOW))

(defn clear_register [board]
  (core/digital-write board _MR LOW)
  (core/digital-write board _OE LOW)
  (acc/impulse board ST-CP)
  (core/digital-write board _MR HIGH))

(defn shift-register [board & {:keys [how-much wait]
                               :or {how-much 8 wait 100}}]
  (doseq [_ (range 0 how-much)]
    (acc/impulse board SH-CP :wait wait)
    (acc/impulse board ST-CP :wait wait)))

(defn sens1
  "One led only, shifting left"
  [board speed]
  (clear_register board)
  (core/digital-write board DATA HIGH)
  (shift-register board :wait speed :how-much 1)
  (core/digital-write board DATA LOW)
  (shift-register board :wait speed :how-much 8))

(defn sens2
  "One led only, shifting right"
  [board]
  (doseq [led (range 7 -1 -1)]
    (clear_register board)
    (core/digital-write board _OE HIGH)
    (core/digital-write board DATA HIGH)
    (shift-register board :wait 0 :how-much 1)
    (core/digital-write board DATA LOW)
    (shift-register board :wait 0 :how-much led)
    (core/digital-write board _OE LOW)
    (Thread/sleep 50)))

(defn register-load [board i]
  (doseq [x (range 0 8)]
    (core/digital-write board
                        DATA
                        (if (bit-test (bit-shift-left i x) 7) HIGH LOW))
    (shift-register board :how-much 1)))

(defn vagues [board]
  (doseq [speed [20 10 5 2]
          value [HIGH LOW]]
    (core/digital-write board DATA value)
    (shift-register board :wait speed)))

(comment
  (utils/list-ports)
  (def board (utils/connect pin-mapping :debug false))

  board

  ;; just integrated led test
  (acc/integrated-led-blink board)

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

  (core/digital-write board DATA HIGH)
  (core/digital-write board DATA LOW)
  (shift-register board :how-much 8)

  (utils/close-board board)
  ;;
  )