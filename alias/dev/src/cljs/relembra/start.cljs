(ns ^:figwheel-no-load relembra.start
  (:require
   [relembra.core :as core]
   [devtools.core :as devtools]))


(extend-protocol IPrintWithWriter
  js/Symbol
  (-pr-writer [sym writer _]
    (-write writer (str "\"" (.toString sym) "\""))))

(enable-console-print!)

(print "started!")

(devtools/install!)

(core/init! true)
