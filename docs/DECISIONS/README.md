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
