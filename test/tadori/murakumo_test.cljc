(ns tadori.murakumo-test
  (:require [clojure.test :refer [deftest is testing]]
            [tadori.murakumo :as tadori]))

(def full-attestations
  (into {}
        (map (fn [gate] [gate (str "attested-" (name gate))]))
        (distinct (mapcat :required-gates (vals tadori/cell-specs)))))

(deftest maps-all-legacy-tadori-cells
  (is (= #{"tadori_address_label"
           "tadori_attribution_join"
           "tadori_case_intake"
           "tadori_silen_tadori_review"
           "tadori_transparent_force_log"
           "tadori_tx_trace"}
         (set (map :legacy-cell (vals tadori/cell-specs))))))

(deftest r0-gates-block-effects
  (let [plan (tadori/cell-plan :case-intake
                               {:case-id "case-001"
                                :authorization-ref "auth-001"
                                :computed-at "2026-06-29T00:00:00Z"})]
    (is (= :blocked (:status plan)))
    (is (= [:council-charter-attestation
            :silen-tadori-baseline-review
            :active-case-mandate-baseline
            :authorization-ref-baseline
            :transparent-force-log-baseline
            :open-source-system-of-record-baseline
            :pii-encrypted-envelope-baseline
            :evidence-only-no-enforcement-baseline
            :no-platform-held-key-baseline
            :murakumo-only-inference-baseline
            :kotoba-only-substrate-baseline
            :no-mass-surveillance-baseline
            :no-adherent-deanon-baseline
            :authority-signature-baseline
            :case-valid-window-baseline
            :phase0-dry-run-if-no-case-baseline]
           (:missing-gates plan)))
    (is (empty? (:effects plan)))))

(deftest attested-attribution-emits-encrypted-finding
  (let [plan (tadori/cell-plan :attribution-join
                               {:attestations full-attestations
                                :case-id "case-001"
                                :trace-id "trace-001"
                                :finding-id "finding-001"
                                :target-address "0xabc"
                                :computed-at "2026-06-29T00:00:00Z"
                                :record {:tid "finding-001"
                                         :objectKind "ip-obs"}})
        effect (first (:effects plan))]
    (is (= :ready (:status plan)))
    (is (= :mst/put-record (:op effect)))
    (is (= tadori/actor-did (:actor effect)))
    (is (= "com.etzhayyim.tadori.attributionFinding" (:collection effect)))
    (is (= "finding-001" (:rkey effect)))
    (is (= true (get-in effect [:record :encrypted])))
    (is (= true (get-in effect [:record :evidenceOnly])))
    (is (= 0 (get-in effect [:record :enforcementActionCount])))))

(deftest silen-review-keeps-zero-counters
  (let [plan (tadori/cell-plan :silen-tadori-review
                               {:attestations full-attestations
                                :review-id "review-2026q2"
                                :computed-at "2026-06-29T00:00:00Z"})
        record (:record (first (:effects plan)))]
    (is (= :ready (:status plan)))
    (doseq [k [:noncaseWriteCount
               :plaintextPiiCount
               :proprietarySorCount
               :enforcementActionCount
               :platformHeldKeyCount
               :murakumoBypassCount
               :massSurveillanceCount
               :adherentDeanonCount
               :nonKotobaStoreCount]]
      (is (= 0 (get record k)) (str k " must remain zero")))))

(deftest cell-specific-gates-remain-specific
  (testing "tx trace keeps malak contract"
    (let [attestations (dissoc full-attestations :malak-wallet-deep-inspect-contract-baseline)
          plan (tadori/cell-plan :tx-trace {:attestations attestations})]
      (is (= [:malak-wallet-deep-inspect-contract-baseline] (:missing-gates plan)))))
  (testing "address labels keep proprietary-SoR exclusion"
    (let [attestations (dissoc full-attestations :proprietary-sor-count-zero-baseline)
          plan (tadori/cell-plan :address-label {:attestations attestations})]
      (is (= [:proprietary-sor-count-zero-baseline] (:missing-gates plan)))))
  (testing "transparent force log keeps no-enforcement action"
    (let [attestations (dissoc full-attestations :no-enforcement-action-baseline)
          plan (tadori/cell-plan :transparent-force-log {:attestations attestations})]
      (is (= [:no-enforcement-action-baseline] (:missing-gates plan))))))

(deftest all-cell-plans-ready-when-attested
  (let [plans (tadori/all-cell-plans {:attestations full-attestations
                                      :case-id "case-001"
                                      :authorization-ref "auth-001"
                                      :trace-id "trace-001"
                                      :report-id "report-001"
                                      :finding-id "finding-001"
                                      :review-id "review-2026q2"
                                      :target-address "0xabc"
                                      :owner-did "did:example:owner"
                                      :authority-did "did:example:authority"
                                      :computed-at "2026-06-29T00:00:00Z"})]
    (is (= (set (keys tadori/cell-specs)) (set (keys plans))))
    (is (every? #(= :ready (:status %)) (vals plans)))
    (is (= 6 (count (mapcat :effects (vals plans)))))))
