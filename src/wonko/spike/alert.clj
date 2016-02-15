(ns wonko.spike.alert)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; This namespace will configure, and if necessary, send alerts via    ;;
;; email, pager-duty, etc. Each of which can be under this. See        ;;
;; kits.alerts and eccentrica.utils.alerts namespaces for integrations ;;
;; with pager duty.                                                    ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn pager-duty [{:keys [metric-type metric-name options] :as event}]
  (if (:alert options)
    (spit "wonko.log" (str "Paging someone. This bad thing happened:" metric-name) :append true)))
