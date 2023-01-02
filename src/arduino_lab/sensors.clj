(ns arduino-lab.sensors
  "╭─────────────────╮
   │ Arduino sensors │
   ╰─────────────────╯"
  (:require [clodiuno.core :as core :refer [LOW HIGH INPUT OUTPUT PWM]]
            [clodiuno-debug.utils :as utils]))

(defn pwm-demo [board]
  (core/pin-mode board 12 PWM)
  (doseq [i (concat (range 0 255 4) (range 255 0 -4))]
    (core/analog-write board 12 i)
    (Thread/sleep 10)))

(defn tri-color
  "Tri-color LED module.
  Can be inserted directly on pins 11, 12, 13 and ground of the Arduino."
  [board]
  (core/pin-mode board 11 PWM)
  (core/pin-mode board 12 PWM)
  (core/pin-mode board 13 PWM)
  (doseq [i (concat (range 0 255 1) (range 255 -1 -1))]
    (doall (map #(core/analog-write board % i) [11 12 13]))
    (Thread/sleep 1)))

(defn seven-color-flash
  "7 Color Flash.
  Set the - to ground; the middle pin is not connected.
  This LED automatically switches between 7 colors.
  The LED has a integrated IC.
  If you disconnect the LED the colors will reset and the animation
  will start over."
  [board]
  (core/pin-mode board 12 OUTPUT)
  (core/digital-write board 12 HIGH)
  (println "stops flashing in 10 seconds")
  (Thread/sleep 10)
  (core/digital-write board 12 LOW))

(defn two-color-led
  "Two-Color LED (big one)"
  [board]
  (core/pin-mode board 12 PWM)
  (core/pin-mode board 13 PWM)
  (doseq [i (range 0 255 1)]
    (core/analog-write board 12 i)
    (core/analog-write board 13 (- 255 i))
    (Thread/sleep 10))
  (doseq [i (range 0 255 1)]
    (core/analog-write board 12 (- 255 i))
    (core/analog-write board 13 i)
    (Thread/sleep 10)))

; Temperature Sensor
; 18B20 de 7Q-Tek
; it's not the easiest: you have to implement one-wire (it's not I2C)

(defn sound-sensor
  "Big Sound Sensor KY-037 et Small Sound Sensor KY-038
  pins: Analog A0, Ground, Vcc, Digital D0
  There is in the module an LM393 - Dual Comparators.
  You have to turn the variable resistors to adjust the sensitivity,
  in particular the potentiometer until the limit of lighting
  of the integrated green led to be at the triggering threshold."
  [board]
  (core/pin-mode board 13 OUTPUT)
  (core/enable-pin board :analog 0)
  (while true
    (let [a0 (core/analog-read board 0)]
      (if (> a0 575)
        (core/digital-write board 13 HIGH)
        (core/digital-write board 12 LOW))))
  (core/pin-mode board 13 OUTPUT)
  (core/enable-pin board :digital 2)
  (core/pin-mode board 2 INPUT)
  (while true
    (let [a0 (core/digital-read board 2)]
      (println a0)
      (if a0
        (core/digital-write board 13 HIGH)
        (core/digital-write board 12 LOW)))))

(defn raw-to-celcius
  "Using Steinhart-Hart equation and coefficients for thermistor"
  [val]
  (let [balance-resistor-ohm 10110
        a 0.001129148
        b 0.000234125
        c 0.0000000876741
        t (->> (float val) (/ 1024.0) dec (* balance-resistor-ohm) Math/log)
        t (/ 1 (+ (* t t t c) (* t b) a))
        t (- t 273.15)]
    t))

(defn beta-to-celcius
  "Measurement of the balance resistance on breadboard: 10.11kOhm.
   The thermistor is 112kOhm at room temp.
   22°C i.e. 295.15°K."
  [val {:keys [beta] :or {beta 3950}}]
  (let [balance-resistor-ohm 10110
        thermistor-ohm 112600
        room-temperature 295.15
        max-adc 1024.0
        rth (* balance-resistor-ohm (dec (/ max-adc val)))
        t (/ (* beta room-temperature)
             (+ beta (* room-temperature (Math/log (/ rth thermistor-ohm)))))
        t (- t 273.15)]
    t))

; rThermistor = BALANCE_RESISTOR * ((MAX_ADC / val) - 1);
; tKelvin = (BETA * ROOM_TEMP) /
; (BETA + (ROOM_TEMP * log (rThermistor / RESISTOR_ROOM_TEMP)));

(defn analog-temperature-sensor
  "KY-013 Analog Temperature Sensor ou MK52-3950?.
   The temperature sensor is a NTC thermistor.
   Connect the left pin (S) to A0, VCC in the middle and ground.
   NTC stands for 'Negative Temperature Coefficient'
   which means that the sensor resistance will decrease
   as temperature increases.
   Temperature measurement range  -55°C to 125°C [-67°F to 257°F]
   Measurement Accuracy  ±0.5°C

   At this temperature the value read, as indicated by the curve,
   should be 481, but we are at 937, hence the offset of 456.

   Processing (Quil) is perhaps better adapted to live graphics?

   https://arduinomodules.info/ky-013-analog-temperature-sensor-module/"
  [board]
  (core/enable-pin board :analog 0)
  (doseq [_i (range 0 100)]
    (let [a0 (core/analog-read board 0)]
      (println a0 (raw-to-celcius a0) (raw-to-celcius (- a0 456))))
    (Thread/sleep 100))
  (core/disable-pin board :analog 0))

(defn temp-data []
  (for [i (range 1 1023)]
    {:value i :item i :temp (raw-to-celcius i)}))

(def temp-plot
  {:data {:values (temp-data)}
   :width "1024"
   :height "1024"
   :encoding {:x {:field "value" :type "quantitative"}
              :y {:field "temp" :type "quantitative"}}
   :mark "line"})

(defn beta-data [opts]
  (for [i (range 1 1023)]
    {:value i :item i :temp (beta-to-celcius i opts)}))

(def beta-plot
  {:data {:values (beta-data {})}
   :width "1024"
   :height "1024"
   :encoding {:x {:field "value" :type "quantitative"}
              :y {:field "temp" :type "quantitative"}}
   :mark "line"})

(def all-plots
  [:div
   [:h1 "Look ye and behold"]
   [:p "A couple of small charts"]
   [:div {:style {:display "flex", :flex-direction "row"}}
    [:vega-lite
     {:data {:values (beta-data {})
             :name "beta"}
      :width "512"
      :height "512"
      :encoding {:x {:field "value" :type "quantitative"}
                 :y {:field "temp" :type "quantitative"}}
      :mark "line"}]
    [:vega-lite
     {:data {:values (temp-data)
             :name "Steinhart-Hart"}
      :width "512"
      :height "512"
      :encoding {:x {:field "value" :type "quantitative"}
                 :y {:field "temp" :type "quantitative"}}
      :mark "line"}]]
   [:div {:style {:display "flex", :flex-direction "row"}}
    [:vega-lite
     {:data {:values (beta-data {:beta 3000})
             :name "beta"}
      :width "512"
      :height "512"
      :encoding {:x {:field "value" :type "quantitative"}
                 :y {:field "temp" :type "quantitative"}}
      :mark "line"}]]])

;; ----------------------------------------------------------------------------

(comment

  ;; connect board
  (def board (utils/connect [] :debug false))
  board
  ;; just integrated led test
  (utils/integrated-led-blink board)

  (utils/close-board board)
  ;;
  )