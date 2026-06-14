# 0003 — Calibrate OKLCH tokens to the committed splash hex

- **Status:** Accepted
- **Date:** 2026-06-14
- **Deciders:** Pranav (PM), Claude (engineering)
- **Related:** PR2 (theme + atoms), [0001](0001-tech-stack.md), `colors.xml`, `OklchTest`

## Context

The design canvas specifies colours in OKLCH; `res/values/colors.xml` carries
three of them as committed sRGB hex to seed the pre-Compose splash:
`bg0 #14181B`, `textHi #F9FAFA`, `accent #33C798`. The original comments labelled
these as, e.g., `oklch(0.16 0.008 250)`.

When PR2 built the real OKLCH → sRGB helper and rendered those labels, they did
**not** reproduce the committed hex:

| Token | Label | Label renders to | Committed hex |
|---|---|---|---|
| bg0 | oklch(0.16 0.008 250) | #0B0E11 | **#14181B** |
| textHi | oklch(0.98 0.004 250) | #F6F9FB | **#F9FAFA** |
| accent | oklch(0.76 0.16 165) | #00D097 | **#33C798** |

The accent is the decisive one: `oklch(0.76 0.16 165)` sits at the sRGB gamut
edge and clips to a harsh near-pure `#00D097`, whereas the committed `#33C798`
is a deliberately muted emerald-teal that matches the app's calm, non-gamified
tone. So the committed hex are the real design intent; the OKLCH labels were
approximate descriptions, not exact sources.

Left unreconciled, the Compose surface and the splash would differ (a visible
cold-start flash, and a different accent than the one chosen).

## Decision

Treat the committed hex as the source of truth for the three anchors. Keep OKLCH
as the construction system for the *derived* tones (the `bg1..bg3`/`line` ramp,
the text ramp, the other jewels, category dots), but **calibrate** each anchor's
OKLCH input to round-trip to its exact committed byte value:

- `bg0` → `oklch(0.2061 0.0086 240.33)` = #14181B
- `textHi` → `oklch(0.9843 0.0011 197.14)` = #F9FAFA
- `Jewel.Emerald` → `oklch(0.7422 0.1399 166.58)` = #33C798

`OklchTest` asserts the theme tokens render to the committed hex within ±1 per
channel, so splash and Compose can never silently diverge again.

## Consequences

- **Good:** Splash and first Compose frame are identical. The accent is the
  intended #33C798. The perceptual ramp (derived tones) still benefits from
  OKLCH. The invariant is test-enforced.
- **Bad:** The anchor OKLCH numbers are no longer "round" — they read as derived
  constants. Mitigated by the rendered-hex comment beside each.
- **Neutral:** If a future design canvas supplies a full, internally-consistent
  OKLCH palette, these three calibrations can be replaced wholesale and the test
  updated to the new hex.

## Alternatives considered

- **Trust the OKLCH labels, regenerate the hex** — rejected. Would change the
  background to #0B0E11 and the accent to #00D097, discarding deliberate design
  choices for the sake of round numbers.
- **Drop OKLCH, hard-code every colour as hex** — rejected. Loses the
  perceptually-even derivation that makes the neutral ramp and jewels coherent.
- **Loosen `OklchTest` to a wide tolerance** — rejected. It would mask exactly
  the splash/Compose divergence the test exists to catch.
