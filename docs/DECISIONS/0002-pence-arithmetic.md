# 0002 — Give Pence arithmetic operators, but not division

- **Status:** Accepted
- **Date:** 2026-06-14
- **Deciders:** Pranav (PM), Claude (engineering)
- **Related:** PR2 (theme + atoms), [ARCHITECTURE §7](../ARCHITECTURE.md), [0001](0001-tech-stack.md)

## Context

`core/money/Pence` is the value type every amount in Tally passes through. The
architecture doc flagged one open question to settle when `Pence` landed
(ARCHITECTURE §7): should it expose arithmetic operators (`+`, `-`, scalar `*`)
or only explicit `add`/`subtract` methods?

Two forces pull against each other. A money type you can't add reads badly at
call sites (`a.add(b).subtract(c)` versus `a + b - c`), and because `Pence` is a
`@JvmInline value class` over `Long`, the operators compile to plain `long`
arithmetic — zero overhead. But money division is genuinely dangerous: splitting
£10 three ways and dropping the spare penny is a real bug, and an innocent-
looking `/` operator invites exactly that.

## Decision

`Pence` gets `plus`, `minus`, `unaryMinus`, and scalar `times` (`Int`/`Long`)
operators, plus `compareTo`. It does **not** get a division operator. Splitting
an amount goes through `Pence.splitEvenly(parts)`, which distributes remainder
pennies across the leading shares so the parts always sum back to the original.

Formatting and parsing live in a separate `MoneyFormat` object (GBP-only, hard-
coded rules, integer-only maths) rather than on the value class, keeping `Pence`
itself a thin arithmetic type.

## Consequences

- **Good:** Call sites read like maths. No floating point anywhere near money
  (the detekt no-float rule planned for `core/money/` in M2 stays satisfiable).
  `splitEvenly` makes the only lossy money operation explicit and tested.
- **Bad:** A caller who wants a ratio still has to reach into `.value`; we accept
  that friction as the price of not shipping a footgun `/`.
- **Neutral:** `times` takes a scalar, not another `Pence` — multiplying two
  money values is meaningless and won't compile.

## Alternatives considered

- **Explicit `add`/`subtract` only** — rejected. Verbose at every call site for
  no safety gain over operators on an integer type.
- **A full `Money`/`Currency` abstraction** — rejected. Single-currency by
  constitution; multi-currency is explicitly out of scope (CONTRIBUTING §16).
- **A division operator with documented rounding** — rejected. Even documented,
  `/` hides the dropped-penny decision. `splitEvenly` surfaces it.
