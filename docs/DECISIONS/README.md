# Architecture Decision Records

Every non-trivial architectural decision lands here as a numbered ADR. The
Definition of Done (PM brief §15.8) makes this enforceable: a feature isn't
done until either there's no architectural choice, or there's an ADR.

## When to write one

Write an ADR when the decision:

- changes the shape of the code in a way a future reader couldn't guess from the source (e.g. choosing one library over another similarly capable one);
- locks in a privacy or security tradeoff;
- has a cost to reverse (e.g. a DB schema choice, a serialisation format);
- contradicts the PM brief or the constitution (and was approved — record the approval).

Skip it for: choices that read directly from the brief, version bumps, refactors that don't change behaviour, formatting decisions captured by ktlint/detekt.

## Template

```markdown
# NNNN — Short imperative title

- **Status:** Proposed | Accepted | Superseded by [NNNN](...) | Deprecated
- **Date:** YYYY-MM-DD
- **Deciders:** Pranav (PM), Claude (engineering)
- **Related:** PR #NN, brief §X, [NNNN](...)

## Context

What forces are at play. Two or three short paragraphs. Quote the brief if
relevant.

## Decision

The single sentence that captures what we chose. Then a paragraph that
elaborates.

## Consequences

- Good: ...
- Bad: ...
- Neutral / future work: ...

## Alternatives considered

- **X** — why not.
- **Y** — why not.
```

## Index

- [0001 — Tech stack selection](0001-tech-stack.md)
- [0002 — Give Pence arithmetic operators, but not division](0002-pence-arithmetic.md)
- [0003 — Calibrate OKLCH tokens to the committed splash hex](0003-oklch-token-calibration.md)
- [0004 — Paparazzi screenshots via JUnit Vintage; goldens excluded on CI for now](0004-paparazzi-screenshots.md)
- [0005 — Transaction amount is signed in spend terms](0005-transaction-amount-sign.md)
- [0006 — Drop the splits feature; log your own share](0006-drop-splits.md)
- [0007 — Categorisation is deterministic and local; learned rules in DataStore](0007-categorisation.md)
- [0008 — Ship the core; defer SMS/CSV import, biometric, and export](0008-ship-core-defer-rest.md)
- [0009 — Auto-capture spending from payment notifications](0009-notification-ingestion.md)
