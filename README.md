# Tally

Single-user, GBP-only, dark-themed Android personal expense tracker. Manual entry + opt-in SMS ingestion + 22:00 daily reminder + deterministic insights. Local-first, biometric-locked, no telemetry, observational voice.

> **Status:** in development. M1 in progress — see [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the current shape and [`docs/DECISIONS/`](docs/DECISIONS/) for ADRs.

## Quick start (local dev)

You need:

- **JDK 17** (Temurin preferred). Verify with `java -version`.
- **Android Studio** (Ladybug or newer) — bundles the Android SDK and a JDK 17.
- A device or emulator on Android 8.0+ (API 26+).

```bash
git clone https://github.com/Jenil1320/Tally.git
cd Tally

# One-time: bootstrap the Gradle wrapper if missing.
# (Android Studio's Sync will also do this.)
gradle wrapper --gradle-version 8.10.2 --distribution-type bin

# Pull Geist fonts (binaries are git-ignored; see docs/DECISIONS/0001).
./scripts/fetch-geist-fonts.sh        # macOS/Linux
pwsh ./scripts/fetch-geist-fonts.ps1  # Windows

# Build a debug APK.
./gradlew :app:assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/`. Drag-drop to your device or use Android Studio's Run.

## Project layout

```
tally/
├── app/                         # the only application module in v1.0
│   └── src/main/kotlin/com/tally/
│       ├── core/                # money (Pence), time, id, crypto primitives
│       ├── data/                # Room, repositories, SMS parser, importer
│       ├── domain/              # pure-Kotlin model + use cases
│       ├── ui/                  # theme, components (T-prefixed), screens
│       └── work/                # WorkManager workers (22:00 reminder etc.)
├── build-logic/                 # Gradle convention plugins (tally.android.*)
├── docs/                        # ARCHITECTURE, PRIVACY, DECISIONS/
├── scripts/                     # fetch-geist-fonts.{sh,ps1}
└── gradle/libs.versions.toml    # all pinned versions
```

## The constitution

Read [`docs/PRIVACY.md`](docs/PRIVACY.md) and §2 of the PM Handoff before contributing.

- Dark only. GBP only. Integer pence (no `Float`).
- No analytics SDKs. CI fails the build if one appears.
- No chart libraries. Charts hand-rolled in Compose Canvas.
- SMS body never leaves the device. `smsRaw` is encrypted at rest and excluded from any future cloud-sync payload.
- 44 dp minimum hit target.
- Biometric prompt on cold start.
- Observational voice. No emoji rewards, no exclamation marks, no streak gamification.

## Milestones

| Tag | Scope |
|---|---|
| `v0.1-m1` | Foundation: skeleton, theme, atoms, nav, Room v1, seed transaction. |
| `v0.2-m2` | Capture: Add sheet, keypad, 22:00 reminder. (Splits dropped — see ADR-0006.) |
| `v0.3-m3` | Insights: categories, trend, rhythm, forecast. |
| `v0.4-m4` | SMS & import. |
| `v0.5-m5` | Polish: outliers, monthly report, biometric, export. |
| `v0.6-m6` | Hardening: cold-start budget, internal Play track. |

## Licence

Source: TBD. Bundled Geist fonts: SIL Open Font Licence 1.1 (see Vercel's [geist-font](https://github.com/vercel/geist-font) release).
