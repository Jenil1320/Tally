# Contributing

This is a single-user app, but the contribution flow is set up like a small team's so it stays disciplined.

## Branches and tags

- `main` is always green and always installable.
- Each milestone ships as a tag: `v0.1-m1`, `v0.2-m2`, …
- PRs branch off `main` as `m{milestone}/{shortname}` — e.g. `m1/pr2-theme-atoms`.

## Pull request checklist (the Definition of Done from §15 of the brief)

Every PR's description must confirm:

- [ ] Code compiles. `./gradlew :app:assembleDebug` passes locally.
- [ ] Static analysis passes. `./gradlew ktlintCheck detekt`
- [ ] Unit tests written and passing. Coverage target met for the touched code.
- [ ] Paparazzi screenshot tests pass on the 3 reference devices (Pixel 4a, Pixel 7 Pro, fold).
- [ ] ViewModel state transitions are documented in the file's KDoc.
- [ ] Accessibility: 44 dp min hit target, semantics set, content descriptions on every non-text-labelled control.
- [ ] No new analytics SDKs introduced. (CI enforces this.)
- [ ] A 30-second screen capture is attached for any user-visible change.
- [ ] An ADR row exists in `docs/DECISIONS/` if an architectural choice was made.

## Style

- Composables are prefixed `T` (`TCard`, `TNum`, `TCatDot`, …). This matches the design canvas and helps grep.
- All money is `Pence` (a `@JvmInline value class` wrapping `Long`). No `Double` or `Float` in any package under `core/money/`, `data/`, or `domain/`. detekt enforces this.
- Every digit on screen renders in Geist Mono with `tabular-nums`. Use `TNum` — never `Text("£${n}")`.
- One file per use case in `domain/usecase/`. Each is one `suspend operator fun invoke(...)` plus its types.

## Commit messages

Conventional Commits style: `feat(scope): summary` / `fix(scope): summary` / `chore: summary` / `docs: summary`. Examples:

```
feat(theme): translate canvas oklch tokens into Compose
fix(sms): drop duplicate parses within ±60 s window
docs(privacy): expand "what we never do"
```

## Out of scope (don't add without a new conversation)

See §16 of the PM brief. Briefly: investments, income tracking, multi-currency, savings goals, badges/streaks, AI chat, multi-user, subscriptions discovery, bill negotiation, web/iOS.
