(ns ^:figwheel-no-load relembra.start
  (:require
   [relembra.core :as core]))


(extend-protocol IPrintWithWriter
  js/Symbol
  (-pr-writer [sym writer _]
    (-write writer (str "\"" (.toString sym) "\""))))

(core/init! false)
