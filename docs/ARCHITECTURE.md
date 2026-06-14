# Tally — Architecture

> **Status:** living document. Each milestone ends with an update to this file when an architectural choice is made or revised. ADRs in `DECISIONS/` capture the *why*; this file captures the *what is*.

## 1 · Shape

```
┌────────────────────────────────────────────────────────────────────────────┐
│  ui/        (Compose: theme tokens, atoms with the T-prefix, screens)      │
│   ↑   StateFlow<UiState>                                                   │
│  domain/    (pure Kotlin: model + use cases. No Android imports.)          │
│   ↑   suspend fun invoke(...)                                              │
│  data/      (Room, repositories, SMS parser, CSV importer, categoriser)    │
└────────────────────────────────────────────────────────────────────────────┘
                    ↓                                          ↓
                Room DB                              Android Keystore + Tink
              (per-table indices,                    (DEK wraps smsRaw and
               soft delete, ULIDs)                    biometric prefs)
```

One Gradle module (`app`) for v1.0. Multi-module split is on the table for M6 only if cold start measurement demands it; until then the single-module rule keeps refactors cheap.

## 2 · UDF flow

- **Source of truth:** Repositories in `data/repo/`. Every screen subscribes via `Flow` and never holds derived state in the ViewModel that the repo couldn't recompute.
- **State:** `data class XyzUiState` per screen, exposed as `StateFlow<XyzUiState>`. Marked with a single sealed `LoadState` member (`Idle | Loading | Ready | Error`).
- **Events:** ViewModels receive intents as ordinary `suspend fun onFoo()` methods. No `Channel<Intent>` indirection in v1 — KISS.
- **Navigation:** type-safe Compose Navigation. Routes live in `ui/navigation/Routes.kt` as `@Serializable` data classes.

## 3 · Modules of note

- **`core/money/`** — `Pence` value class (`@JvmInline value class Pence(val value: Long)`), plus `MoneyFormat` (GBP-only format/parse). Landed in PR2 with arithmetic operators (`+`, `-`, unary `-`, scalar `*`) but no division — splitting goes through `splitEvenly` so remainder pennies reconcile ([ADR-0002](DECISIONS/0002-pence-arithmetic.md)). Every digit on the wire and on screen passes through here. Float arithmetic in this package is a compile-time-detectable detekt violation (rule added M2).
- **`ui/theme/`** — the design system landed in PR2: an `oklch()` OKLCH→sRGB helper, the `TallyColors`/`TallyTypography`/`TallyShapes`/`TallySpacing` token sets exposed via `TallyTheme`, and the `Jewel` accent enum. The three committed splash anchors (`colors.xml`) are reproduced by *calibrated* OKLCH inputs ([ADR-0003](DECISIONS/0003-oklch-token-calibration.md)). Atoms (`T`-prefixed) live in `ui/components/`.
- **`core/time/`** — `Clock` interface. `SystemClock` is the only production implementation; tests inject `FakeClock`.
- **`core/id/`** — `UlidGenerator` (Crockford base32, monotonic within a millisecond, 26 chars). Used as PK on every table.
- **`core/crypto/`** — `SmsRawEncryptor` wraps Tink's `Aead` with a Keystore-backed DEK. The cipher is `AES-256-GCM`. The associated data is the transaction ULID, so a smsRaw payload that's been moved between rows fails to decrypt.

## 4 · DI graph (Hilt)

- `@Singleton`: `Database`, every DAO, every Repository, `SmsRawEncryptor`, `Clock`, `UlidGenerator`.
- `@ViewModelScoped`: nothing in v1. ViewModels get their dependencies directly.
- `@HiltAndroidApp` on `TallyApp` so `WorkManager` picks up Hilt-injected workers via the in-app `Configuration.Provider`.

## 5 · Threading

Coroutines + Flow only. Two app-scoped dispatchers exposed via Hilt qualifiers:

- `@IoDispatcher` — DB and parsing.
- `@DefaultDispatcher` — CPU work (analytics formulas).

UI flows are collected on `Dispatchers.Main.immediate` by default via Compose.

## 6 · Migrations

Room migrations live in `data/db/migrations/`. Every release branch must include a migration test that walks `1 → currentVersion`. CI fails the release build if a `destructiveMigrationFallback` reaches `release` (asserted by a test).

## 7 · Open architectural questions

- Whether to expose Room entities directly to the domain layer or map through a separate `domain/model` set. Current plan: thin mapping in repositories, full DTO separation only if a Room column type ever leaks an Android dependency upward. Re-evaluate at M3 when analytics queries get spicy.
- ~~Whether `Pence` deserves arithmetic operators or only explicit `add`/`subtract`.~~ **Resolved in PR2:** operators yes, division no — see [ADR-0002](DECISIONS/0002-pence-arithmetic.md).

— Last updated: M1 PR2.
