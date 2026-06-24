(ns tadori.tests.test-malak-contract
  "tadori 辿 — the malak→tadori traceReport CONTRACT is executable: the reference fixture that
  malak (external repo) must emit consolidates cleanly through the seam. ADR-2605301400 §D3."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [tadori.methods.malak-ingest :as malak]))

(def ^:private mandate
  {:case-id "case:reference" :authorization-ref "warrant:ref"
   :authority-did "did:web:etzhayyim.com:authority:synthetic" :phase 1 :opened-ts 20260617})

(defn- contract []
  (edn/read-string (slurp (io/resource "tadori/kotoba/malak-trace-report.contract.edn"))))

(deftest reference-contract-consolidates
  (let [{:keys [analysis datoms]} (malak/consolidate (contract) mandate)]
    (is (= :mixer (get (:classes analysis) "0xfeedface00000000000000000000000000000000"))
        "tadori re-derives the mixer from the contract's txs (SoR)")
    (is (contains? (set (get-in analysis [:flow :exits])) "0x1234567890abcdef1234567890abcdef12345678"))
    (is (pos? (count datoms)))))

(deftest reference-contract-passes-validation
  (is (map? (malak/validate-report (contract))) "the reference traceReport is contract-valid"))
