# com.etzhayyim.tadori.* — Lexicons

Lexicon schemas for **辿 tadori** — authorized on-chain transaction tracing +
actor attribution (ADR-2605301400). kotoba-EAVT-native; R0 = schema skeletons
(R1 adds `additionalProperties: false` + required-field/signature enforcement).

| Lexicon | Purpose | Key invariant |
|---|---|---|
| `caseMandate` | Authorization anchor — every live write references one (G3) | `transparentForceLogged` const true (G5); `phase` 0 dry-run default |
| `attributionFinding` | Cross-store edge addr/cluster → ip-obs/dns-obs/person (VAET 2-hop) | `encrypted` required true for person/IP/device objects (G6) |
| `traceReport` | Durable malak wallet/address trace result | `externalSourcesUsed` records feature-flagged inputs only (G4) |
| `silenTadoriReview` | Quarterly Council audit | 9 structural zero-counters; any nonzero ⇒ halt + chigiri.disputeMediation (G12) |

The 8 EAVT entity classes (`tx`/`addr`/`cluster`/`label`/`case`/`ip-obs`/`dns-obs`/`attribution`)
live as kotoba-kqe datoms (ADR-2605301400 §D2); these Lexicons define the
**authored MST record shapes** (mandate / finding / report / review). Read-only
observation classes (`tx`/`addr`/`ip-obs`/`dns-obs`) are projected from malak /
ipaddress / yabai migrations (T1–T3), not authored here.
