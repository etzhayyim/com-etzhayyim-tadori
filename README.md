# tadori — Authorized On-Chain Tracing Actor

`tadori` is the durable, kotoba-native case graph for authorized on-chain
transaction tracing and actor attribution. It consolidates malak compute output,
ipaddress observations, and yabai CTI into case-scoped evidence datoms. It is
evidence-producing only; enforcement remains outside this actor.

## Migration Boundary

`kotoba-lang/kotodama-cells/tadori_*` is legacy source
during migration. The domain actor implementation belongs here as pure `.cljc`
plans under `src/tadori/murakumo.cljc`: the six tracing cells map to
`caseMandate`, `traceReport`, `attributionFinding`, and `silenTadoriReview` MST
records. The boundary is fail-closed: missing case authorization, Transparent
Force logging, open-source system-of-record, PII encryption, evidence-only,
no-platform-key, Murakumo-only, kotoba-only, no-mass-surveillance, and
no-adherent-deanon attestations produce no write effects. Host placement remains
in `kotoba-lang/murakumo`; any AT Protocol/PDS surface remains in
`gftdcojp/app-aozora`.

## Cells

| Cell | Node | Output |
|---|---|---|
| `tadori_case_intake` | levi | `caseMandate` |
| `tadori_tx_trace` | levi | `traceReport` |
| `tadori_address_label` | levi | `traceReport` |
| `tadori_attribution_join` | levi | `attributionFinding` |
| `tadori_transparent_force_log` | levi | `caseMandate` audit datom |
| `tadori_silen_tadori_review` | levi | `silenTadoriReview` |

## Boundary Invariants

- Authorized investigation only: every live write references an active case.
- Evidence only: no enforcement, takedown, freeze, or adjudication action.
- PII encrypted: person/IP/device attribution uses encrypted envelopes.
- Open-source system of record: proprietary chain-analysis feeds are inputs only.
- No platform-held key: writes are member or community-operator signed.
- Kotoba-only: no yata SQL, RisingWave, Postgres, or SQLite state.
