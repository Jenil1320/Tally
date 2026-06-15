# 0004 — Paparazzi screenshots via JUnit Vintage; goldens excluded on CI for now

- **Status:** Accepted
- **Date:** 2026-06-15
- **Deciders:** Pranav (PM), Claude (engineering)
- **Related:** PR2 (theme + atoms), [0001](0001-tech-stack.md), CONTRIBUTING (DoD), `.github/workflows/ci.yml`

## Context

The Definition of Done (CONTRIBUTING) requires Paparazzi screenshot tests, and
the tech stack ([ADR-0001](0001-tech-stack.md)) picked Paparazzi precisely
because it renders on the JVM with no emulator. PR2 introduces the first atoms,
so it lands the first goldens. Two frictions surfaced:

1. **Test runner.** The project runs unit tests on the JUnit 5 Platform (the
   `android-junit5` plugin). Paparazzi exposes a JUnit 4 `@Rule`, which the
   platform does not run natively.
2. **Cross-OS rendering.** Goldens are recorded on the developer's machine
   (Windows). Paparazzi rasterises the bundled Geist fonts via the host JDK's
   font engine, which differs at the sub-pixel level between Windows and the
   Linux CI runner — so a byte-exact verify can fail on CI against Windows
   goldens even when nothing visually regressed.

## Decision

Add `junit-vintage-engine` so Paparazzi's JUnit 4 tests run on the JUnit 5
Platform alongside the existing unit tests. Record goldens for an atom gallery
on two device sizes (Pixel 4a and a large-phone proxy for the Pixel 7 Pro).

Screenshot tests run and must pass **locally**. On CI they are excluded via
`-PexcludeScreenshotTests` (a `tasks.withType<Test>` filter in
`app/build.gradle.kts`) until the goldens are re-recorded on a Linux runner.
The logic unit tests (Pence, Oklch, …) remain enforced on CI.

## Consequences

- **Good:** Screenshot regression protection exists immediately on the machine
  where the UI is actually developed. Goldens are committed and reviewable.
- **Bad:** CI does not yet guard against UI regressions. Tracked: re-record on
  Linux (a `recordPaparazziDebug` job, or commit Linux goldens) and drop the
  exclusion to enforce in CI.
- **Neutral:** `verifyPaparazziDebug` is no longer a CI step; it is a local
  command. The Geist fonts must be fetched before running it locally.

## Alternatives considered

- **Enforce goldens on CI now** — rejected. Windows-recorded goldens would
  produce false failures on the Linux runner.
- **Keep the old `verifyPaparazziDebug || true`** — rejected. A swallowed-error
  step that can never fail is noise; and the screenshot tests would still fail
  the (non-tolerant) `testDebugUnitTest` step on CI.
- **Robolectric instead of Paparazzi** — rejected. Already settled in ADR-0001;
  Paparazzi needs no emulator and renders real layout.
