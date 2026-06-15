# 0008 — Ship the core; defer SMS/CSV import, biometric, and export

- **Status:** Accepted
- **Date:** 2026-06-15
- **Deciders:** Jenil (owner/PM), Claude (engineering)
- **Related:** M4–M6, [0006](0006-drop-splits.md), [0007](0007-categorisation.md)

## Context

After M1–M3 and the M4 auto-categoriser shipped and were verified running on the
owner's Pixel 7, the app is a complete personal expense tracker: manual capture,
deterministic auto-categorisation, insights (donut/trend/rhythm/forecast), and
the 22:00 reminder. The remaining roadmap — live SMS ingestion, CSV import,
biometric cold-start lock, monthly report, and export — adds reach but not core
function, and the SMS/crypto parts can't be verified without the owner's real
bank texts and on-device Keystore exercise.

## Decision

Stop here and release the core as **`v0.4.0`**. Defer the rest:

- **SMS ingestion** (READ_SMS, parsing, smsRaw Keystore/Tink encryption) — M4.
- **CSV import** — M4.
- **Biometric cold-start lock, monthly report, export** — M5.
- **Cold-start hardening / Play track** — M6.

The owner can reopen any of these as a new milestone; the groundwork is in place
(the categoriser engine is parser-ready, `core/crypto` is reserved in the
architecture, `Pence` export-formatting exists, and the manifest already declares
the SMS/biometric permissions for review).

## Consequences

- **Good:** A finished, installable, useful app now, rather than a half-built
  larger one. Every shipped feature is tested and verified on-device.
- **Bad:** No automatic capture from bank texts yet; entry stays manual (the
  categoriser keeps that fast). No export/biometric yet.
- **Neutral:** The deferred work is documented and scaffolded, so resuming is a
  fresh milestone, not a rediscovery.

## Alternatives considered

- **Build SMS/CSV with caveats now** — declined by the owner: not worth shipping
  unverifiable code for features they don't currently need.
- **Keep going through M5/M6** — declined: diminishing returns versus a clean
  release of the core.
