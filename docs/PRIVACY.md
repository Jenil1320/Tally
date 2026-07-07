# Tally — Privacy

> Plain English. If you can't explain a privacy choice to a non-technical user in two sentences, it's not a privacy choice — it's a compromise.

## What Tally collects

Tally is a single-user app that runs entirely on your phone. The only personal data it touches is:

- Transactions you enter manually.
- Transaction-shaped messages from banks you've explicitly granted SMS access to.
- Payment notifications (e.g. Google Pay) from apps you've explicitly allowed, when you turn on notification capture. Tally reads the notification text, extracts the amount and merchant on-device, and **stores only the resulting transaction** — the raw notification text is not kept. See "Permissions" below.
- A bundled list of merchant→category rules the app learns from your corrections.

That's it. There is no account, no sign-in, no profile, no email collection, no advertising identifier read.

## What Tally never does

- **Never sends transactions, balances, or notes off your phone.** No HTTP traffic in v1.0. CI fails the build if a network call escapes a `release` build outside the allow-list (none exists today).
- **Never sends the raw SMS body anywhere.** Even when Cloud Sync ships in v1.1, the `smsRaw` column is excluded from the sync payload by data class.
- **Never ships analytics / crash-reporting SDKs.** No Firebase Analytics, Amplitude, Mixpanel, Sentry, Bugsnag, Datadog, AppsFlyer, Crashlytics, Flurry, or kin. CI greps the merged dependency tree on every PR and fails if any of them appear. See `.github/workflows/ci.yml`.
- **Never sells, shares, or trades any data.**
- **Never sends push notifications about offers, cashback, or partner content.** The only notifications Tally posts are the user-configurable 22:00 reminder and — when you turn on capture — a local confirmation that an expense was logged (with quick category buttons). Both are generated on-device; nothing is fetched from a server.

## What's encrypted, and how

| What | Where | How |
|---|---|---|
| `smsRaw` (the original SMS text) | Room column on the device | AES-256-GCM via Google Tink, keyed by a Keystore-resident DEK that cannot be exported. The transaction ULID is used as associated data, so a row whose smsRaw has been swapped fails to decrypt. |
| Biometric prefs and reminder config | EncryptedSharedPreferences | Backed by the same Keystore. |
| Database file | Sandboxed app storage | Android FBE (full-disk encryption inherited from the OS). |

## Backup and transfer

- Auto-backup and device-to-device transfer are **disabled** for all Tally data (see `data_extraction_rules.xml` and `backup_rules.xml`).
- You can export your full dataset as JSON or CSV from Settings → Export. The file is written to a location you pick via the system file picker.
- You can wipe everything from Settings → Delete all data (double-confirmed). This clears Room, EncryptedSharedPreferences, and the Keystore DEK; an instrumented test verifies all three are gone.

## Screenshots and the task switcher

`FLAG_SECURE` is on. Balances will not appear in the task-switcher carousel and cannot be screenshotted by other apps. This is set in `MainActivity.onCreate`.

## Permissions

Tally only asks for permissions when you reach the screen that needs them:

- **POST_NOTIFICATIONS** — required for the 22:00 reminder.
- **READ_SMS / RECEIVE_SMS** — only after you tap "Connect SMS" in Settings → SMS sources, and only for senders you specifically allow.
- **Notification access** — only after you turn on "Capture from Google Pay" in Settings and grant it in system settings. This lets Tally read notifications from the payment apps it allow-lists (Google Pay / Wallet) to auto-log spending. It is off by default, you can pause it in-app without revoking the system grant, and it is read-only — Tally never posts, dismisses, or alters those notifications. The text is parsed on-device and discarded; nothing about it leaves your phone. (Technical note: `NotificationListenerService` cannot be scoped to one app by the OS, so the allow-list is enforced in Tally's own code — Tally ignores every notification from every other app.)
- **USE_BIOMETRIC** — for the cold-start biometric prompt.

No other permissions are requested.

## What changes in v1.1 (cloud sync)

If you turn on Cloud Sync (off by default), Tally will end-to-end-encrypt your data with a key derived from a recovery phrase that **only you see**. The phrase is generated on-device and shown once. We do not store it, can not recover it, and can not read your data. The exact key flow ships with an ADR (`DECISIONS/00xx-cloud-sync.md`) and a privacy review checklist.

— Last updated: M1 PR1.
