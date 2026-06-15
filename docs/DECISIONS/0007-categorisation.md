# 0007 — Categorisation is deterministic and local; learned rules in DataStore

- **Status:** Accepted
- **Date:** 2026-06-15
- **Deciders:** Jenil (owner/PM), Claude (engineering)
- **Related:** M4, [0006](0006-drop-splits.md), PRIVACY, CONTRIBUTING §16

## Context

The owner asked whether the app can categorise transactions automatically. The
constitution rules out AI chat and any network calls (local-first, no telemetry),
so categorisation must be **on-device and deterministic** — a rule engine, not a
model. PRIVACY already promises "a bundled list of merchant→category rules the
app learns from your corrections."

## Decision

`suggestCategory(merchant, categories, learnedRules)` (pure, in `domain/category`)
resolves a category by: (1) a **learned** rule for the exact normalised merchant,
then (2) a **bundled** substring rule (`BundledMerchantRules`). Learned rules are
persisted as a **JSON map in DataStore** (`MerchantRuleStore`), not a Room table,
so no schema migration is needed and the feature is fully unit-testable with a
fake. The Add sheet auto-suggests on merchant entry (override-able) and teaches
the mapping on save.

## Consequences

- **Good:** Fully deterministic and offline. No DB migration. The pure engine is
  shared by manual entry now and SMS import later (M4). Easy to test.
- **Bad:** Substring matching is simple — it can mis-hit (e.g. a merchant that
  contains "VUE"). Acceptable: the user's correction immediately learns the right
  mapping, which then wins.
- **Neutral:** If learned rules ever need querying/joining at scale, they can move
  to a Room table via a migration; the `MerchantRuleStore` interface isolates that.

## Alternatives considered

- **An LLM / on-device ML classifier** — rejected: violates the no-AI, no-network
  constitution and is overkill for merchant strings.
- **Learned rules in a Room table** — deferred: needs a migration for marginal
  benefit over a DataStore map at this scale.
