(ns dumpstr.core.util)

(def project-version
  ;; Reading project.clj doen't just work when not running in lein repl
  ;;  (first (drop 2 (read-string (slurp "project.clj"))))
  "0.1.0-SNAPSHOT")
