(ns relembra.data-readers
  ;; Make sure they're already required so we can patch them.
  (:require [time-literals.data-readers]))


(defn patch! []
  (doseq [v (vals (ns-publics 'time-literals.data-readers))]
    (try
      (alter-var-root v (constantly (eval `(fn [~'x] ~(@v 'x)))))
      (catch ClassCastException _ nil))))


(comment

  (time-literals.data-readers/date "2020-08-08")

  (macroexpand-1 '(patch!))

  (patch!)

  )
