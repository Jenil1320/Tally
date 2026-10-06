# 0010 — Log your share of an eating-out bill, as a footnote

- **Status:** Accepted
- **Date:** 2026-10-06
- **Deciders:** Jenil (owner/PM), Claude (engineering)
- **Related:** [0005](0005-transaction-amount-sign.md), [0006](0006-drop-splits.md), [0002](0002-pence-arithmetic.md)

## Context

ADR-0006 dropped "my share". The owner then typed only their own share as the
amount. That has a cost: the cash flow total shows less money than left the bank
account.

The owner now wants both numbers. Cash flow must show the full amount paid.
For eating out, the app must also show how much of that was their own spend.

## Decision

Keep `amount` as the full amount paid. It still drives every cash flow total.

Add one nullable column, `transactions.myShare` (pence). It holds the owner's
own part of the bill. Null means "the whole amount is mine".

- Only the **Eating out** category asks for a share. The Add sheet shows the
  question when that category is chosen: All mine, Half, A third, or Custom.
- Capture notifications for an Eating out payment invite the owner to add a
  share. The payment is still logged first, then edited.
- Totals for the month, the category chart and the monthly report stay on
  `amount`. A separate awareness line reads "Your share of eating out".
- `myShare` must be between 0 and `amount`. The Add sheet caps it. `NewTransaction`
  rejects any other value.
- Any entry with no share counts in full as the owner's own spend. Old
  entries need no change.

## Data safety

Room schema 1 becomes schema 2 through `MIGRATION_1_2`. The migration is a
single `ALTER TABLE ... ADD COLUMN`. It does not touch any row. The
database builder never uses a destructive fallback. An instrumented test writes
rows in schema 1, migrates, and checks that every row is unchanged.

## Consequences

- **Good:** Cash flow stays honest. The owner sees what eating out cost them.
  No existing data changes.
- **Bad:** The category is matched by name ("Eating out"). If the owner renames
  that category, the share question stops showing. Old shares stay stored.
- **Neutral:** Nothing tracks who owes whom. That is still out of scope
  (see ADR-0006).

## Alternatives considered

- **Amount equals share, plus a `paidTotal` column.** This is the path ADR-0006
  suggested. It would change the meaning of `amount` for every old row.
  Rejected, because it risks old totals.
- **Ask for a share on every category.** Rejected. Only eating out is shared in
  practice, and the extra question slows every other entry.
- **Store a share percentage.** Rejected. Pence is exact and matches ADR-0002.
