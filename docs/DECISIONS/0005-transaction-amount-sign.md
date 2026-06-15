# 0005 — Transaction amount is signed in spend terms

- **Status:** Accepted
- **Date:** 2026-06-15
- **Deciders:** Pranav (PM), Claude (engineering)
- **Related:** PR3 (nav + Room v1), [0002](0002-pence-arithmetic.md), CONTRIBUTING §16

## Context

`Transaction.amount` needs a sign convention, and it is a schema-level contract
that is expensive to change once real data exists. Tally is an expense tracker;
income tracking is explicitly out of scope (CONTRIBUTING §16). The only "money
in" is a refund or a card credit, which is best understood as *negative
spending* against a category rather than as income.

## Decision

`amount: Pence` is stored signed in **spend terms**: a purchase is **positive**
(money out), a refund/credit is **negative** (money back). Aggregations like
"spent today/this month" are therefore a plain `sum(amount)`, and a refund
naturally reduces the total. There is no separate `type`/`direction` column.

Display sign is a separate concern handled by `TNum` (it can show a leading `+`
on credits and colour them with `colors.positive`); the stored value is the
single source of truth.

## Consequences

- **Good:** Sums are trivial and correct. One column, no enum to keep in sync.
  Matches the expense-tracker mental model (everything is spend; refunds undo
  spend).
- **Bad:** "Positive = money leaving you" is the opposite of a bank statement's
  convention, so import/SMS parsing (M4) must flip the sign of incoming credits.
  Documented on `Transaction.amount` and enforced by the parser tests when they
  land.
- **Neutral:** If income tracking were ever brought in scope (it is not), this
  convention would need revisiting via a new ADR + migration.

## Alternatives considered

- **Positive magnitude + a `direction` enum** — rejected. Two fields to keep
  consistent, and every aggregation has to branch on direction.
- **Negative = spend (bank-statement convention)** — rejected. Makes the common
  "total spent" a `-sum(...)`, which reads backwards for an expense tracker.
