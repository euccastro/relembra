(ns relembra.data-readers
  ;; Make sure they're already required so we can patch them.
  (:require [time-literals.data-readers]))


(defmacro patch! []
  (list* 'do
         (for [[n f] (ns-publics 'time-literals.data-readers)
               :let [vs (symbol "time-literals.data-readers" (name n))
                     body (f 'x)]]
           `(alter-var-root (var ~vs) (constantly (fn [~'x] ~body))))))


(comment

  (macroexpand-1 '(patch!))


  )
