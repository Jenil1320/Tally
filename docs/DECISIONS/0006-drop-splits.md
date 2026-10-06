# 0006 — Drop the splits feature; log your own share

- **Status:** Accepted. Partly replaced by [0010](0010-eating-out-share.md), which adds an optional share for eating out.
- **Date:** 2026-06-15
- **Deciders:** Jenil (owner/PM), Claude (engineering)
- **Related:** M2, [0002](0002-pence-arithmetic.md), [0005](0005-transaction-amount-sign.md), CONTRIBUTING §16

## Context

The M2 milestone listed "splits". On reaching it we clarified two different
things the word could mean:

1. **Category split** — divide one transaction across categories.
2. **Shared expense ("my share")** — you pay £100 but only £20 is your spend;
   the rest is fronted for friends who repay later, often by covering your share
   of other expenses.

The owner's real need is #2, and the messy part (who owes whom, netting
repayments across months) is a per-person IOU ledger — i.e. **multi-user, which
the brief puts out of scope** (CONTRIBUTING §16).

## Decision

**Do not build a splits feature at all** — neither category splits nor a
shared-expense/"my share" model, and no owed-to-you tracking. The owner logs
**their own share** as the transaction `amount` (type £20, not £100). The
optional note field carries any context ("paid £100 w/ Alex"). Spending totals
are correct by construction because `amount` is already "your spend"
([ADR-0005](0005-transaction-amount-sign.md)).

`Pence.splitEvenly` is retained as a general, tested money primitive (remainder-
safe division) even though no feature now calls it; its cost is nil and it is a
natural operation on the type. Its KDoc no longer references the dropped feature.

## Consequences

- **Good:** M2 shrinks (no splits PR). No multi-user complexity creeps in. The
  data model stays single-field-per-spend and the analytics need no special
  cases. Honours "keep it simple".
- **Bad:** The owner manually knows their share when paying for a group. They
  consider this acceptable (it nets out across months in practice).
- **Neutral:** If a shared-expense model is ever wanted, it returns as a new ADR
  (likely `amount` = share + a `paidTotal` column via a Room migration). The M1
  schema-export/migration scaffolding makes that cheap.

## Alternatives considered

- **"My share" with a `paidTotal` column** — declined by the owner as
  unnecessary complication; netting across months handles it.
- **Category splits** — declined: doesn't match the actual need.
- **Per-friend ledger / settlements** — out of scope (multi-user, CONTRIBUTING
  §16); that is Splitwise's job, not Tally's.
