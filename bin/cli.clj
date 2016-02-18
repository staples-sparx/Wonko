(ns cli
  (:require [wonko.core :as launcher]))

(prn *command-line-args*)
(let [args (apply str (interpose " " *command-line-args*))]
  (prn args)
  (apply launcher/-main *command-line-args*))
