(ns dumpstr.core.util)

(def project-version
  (first (drop 2 (read-string (slurp "project.clj")))))
