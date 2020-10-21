;;; Directory Local Variables
;;; For more information see (info "(emacs) Directory Variables")

((nil
  (cider-default-cljs-repl . figwheel-main)
  (cider-figwheel-main-default-options . "dev"))
 (clojure-mode
  (cider-clojure-cli-global-options . "-A:dev:hotload-deps:test"))
 (clojurescript-mode
  (cider-clojure-cli-global-options . "-A:dev")))
