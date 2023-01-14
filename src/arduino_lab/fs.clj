(ns arduino-lab.fs
  (:require [schema.core :as s]))

(s/defn flowstorm-instrumentation-test [arg]
  (println arg))

;; (require '[arduino-lab.fs] :reload-all)