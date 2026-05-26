# 0001 — Tech stack selection

- **Status:** Accepted
- **Date:** 2026-05-26
- **Deciders:** Pranav (PM), Claude (engineering)
- **Related:** PM brief §3, M1 PR1

## Context

The PM brief §3 prescribes a stack but leaves room for engineering judgement ("Pick this stack unless you have a strong reason otherwise"). Recording the choice here so PR2 onward can refer to a fixed point.

## Decision

Adopt the brief's stack verbatim, with versions pinned in `gradle/libs.versions.toml`:

| Layer | Choice | Pinned version |
|---|---|---|
| Language | Kotlin | 2.0.21 (K2 compiler) |
| Build | Android Gradle Plugin | 8.7.3, Gradle 8.10.2 |
| UI | Jetpack Compose (Material 3) | Compose BOM 2024.12.01, M3 1.3.1 |
| Min SDK / Target SDK | API 26 / API 34 | — |
| Persistence | Room | 2.6.1, KSP 2.0.21-1.0.28 |
| DI | Hilt | 2.52 |
| Async | Coroutines + Flow | 1.9.0 |
| Navigation | Compose Navigation (type-safe) | 2.8.4 |
| Background | WorkManager | 2.10.0 |
| Crypto | Android Keystore + Tink | Tink 1.15.0, security-crypto 1.1.0-alpha06 |
| Biometric | androidx.biometric | 1.2.0-alpha05 |
| Test | JUnit5 + Turbine + Kotest + Paparazzi | JUnit5 5.11.3, Turbine 1.2.0, Kotest 5.9.1, Paparazzi 1.3.5 |
| Static analysis | ktlint + detekt | ktlint plugin 12.1.2, detekt 1.23.7 |
| CI | GitHub Actions | — |

Single Gradle module (`app`) plus `build-logic/` for convention plugins. No multi-module split in v1.0.

## Consequences

- **Good:**
  - Stack matches the brief, so onboarding a second engineer (if it ever happens) is a `clone + setup-java + assemble` sequence with no surprises.
  - Convention plugins keep `app/build.gradle.kts` to ~30 lines; M2/M3 will not become a Gradle config audit.
  - JUnit5 + Turbine + Kotest gives us property-based tests for the categoriser (M4) without a second test runner.
  - Paparazzi runs as a JVM unit test — no emulator needed in CI for screenshot diffs.
- **Bad:**
  - androidx.security-crypto is on `1.1.0-alpha06`; pinning an alpha is normally a smell. Accepted because the stable line is unmaintained and an alpha is what Google ships. Watch for a stable in M6.
  - Paparazzi has a known incompatibility with some Compose previews that use `Modifier.shadow`. Document workarounds in PR2.
- **Neutral:**
  - JDK 17 is required everywhere (CI, local). Documented in `README.md`.

## Alternatives considered

- **Multi-module from day one** — rejected. Single module ships M1 in days, not weeks. Re-evaluate at M6 if cold start ≥ 800 ms on Pixel 4a.
- **MVI with a Channel-based intent bus** — rejected. The brief uses plain MVVM + UDF; adding a Channel layer is ceremony with no payoff for this scope.
- **Truth or AssertJ instead of Kotest** — kept Truth alongside as a comfortable assertion lib; Kotest is for property tests on the categoriser specifically.
- **MockK only / no mocking** — accepted MockK for boundaries we can't fake cheaply (Keystore); use fakes everywhere else.
