# 0009 — Auto-capture spending from payment notifications

- **Status:** Accepted
- **Date:** 2026-07-07
- **Deciders:** Jenil (owner/PM), Claude (engineering)
- **Related:** M4, [0007](0007-categorisation.md), [0008](0008-ship-core-defer-rest.md)

## Context

The owner wants transactions logged automatically. The deferred SMS parser
([0008](0008-ship-core-defer-rest.md)) doesn't help them: their bank does not
send transaction SMS — the signal they *do* get is the **Google Pay / Wallet
notification** on the phone. Android exposes other apps' notifications through a
`NotificationListenerService` (a user-granted "Notification access" capability),
which is exactly how existing expense trackers auto-capture payments.

Two realities constrain the design:

1. **Reliability is best-effort, not guaranteed.** There is no notification
   history API — `getActiveNotifications()` returns only still-showing
   notifications — so any payment notification posted while the listener is
   unbound (OEM battery kills, reboot window) is permanently missed. And in the
   UK on Google Wallet, the tap-to-pay purchase notification has historically
   been posted by Google Play services (`com.google.android.gms`), is silent and
   delayed, and its sender/wording is changing across versions. (India's UPI app
   posts clean per-transaction notifications, but the owner is in London and pays
   in GBP only.)
2. **It touches the privacy constitution.** Reading another app's notifications
   is a new, sensitive data source and a new permission — it must be opt-in,
   local-only, and documented (see PRIVACY.md).

## Decision

Ship notification capture as an **opt-in, best-effort helper** — not a system of
record.

- A `TallyNotificationListenerService` reads an allow-list of payment packages
  (`walletnfcrel`, `nbu.paisa.user`, and `com.google.android.gms` for UK
  tap-to-pay) and delegates to a **pure, unit-tested** `parsePayment` in
  `domain/notify`. Only a clear **GBP debit** is logged; credits, refunds, money
  requests, and promo/reward notifications are rejected. New source enum value:
  `TransactionSource.NOTIFICATION`.
- Captured rows go through the normal `TransactionRepository.add` and the shared
  `suggestCategory` engine, so they are categorised and are **fully editable /
  deletable** like any entry. The owner explicitly accepts "auto-log then edit"
  as the UX, which matches the best-effort reliability.
- Two independent switches: the OS "Notification access" grant (deep-linked from
  Settings) **and** an in-app toggle (`NotificationIngestPreferences`, **off by
  default**), so the user can pause capture without revoking system access.
- **Raw notification text is not persisted in this version.** The `smsRaw`
  column is reserved for at-rest encryption (Tink/Keystore) that is still
  deferred; storing raw text in plaintext would violate the constitution.
  Structured fields only (amount / merchant / date / source). Re-parsing old
  notifications waits for that encryption to land.

### Confirmation + categorise-from-the-shade

Every successful capture posts a local confirmation notification
(`CaptureNotifier`), observational in tone. When auto-categorisation found a
match, it shows the category with a single "Change" action; when it did **not**,
it shows up to **three one-tap category buttons** and the transaction is logged
uncategorised until one is tapped.

- **Why three, not the full list:** Android phones display at most three
  notification actions (verified against the platform docs); `RemoteInput`
  choice-chips exist but cap at ~5 and vary by OEM. Three reliable buttons + a
  tap-through to the full editor is the robust choice (the owner picked this over
  a chip picker or an app-only picker).
- **Which three:** the user's most-used categories (`topCategoryIds` +
  the pure `categoryCandidates`), padded with display order for a fresh install.
- **How the tap is handled:** a plain `BroadcastReceiver`
  (`CategorySelectionReceiver`, dependencies via a Hilt `EntryPoint` rather than
  `@AndroidEntryPoint`, since `BroadcastReceiver.onReceive` is abstract and can't
  call `super`) sets the category off the main thread (`goAsync`), **learns the
  merchant→category rule** (so the merchant auto-categorises next time, exactly
  as saving from the Add sheet does), and updates the notification in place. No
  UI is opened.
- **Full list:** tapping the notification body deep-links (a `MainActivity`
  intent extra threaded into `TallyAppScaffold`, `singleTop`) into the existing
  edit sheet for that transaction, where all categories — and the amount/merchant
  — can be corrected.

### Confirmed real format (Google Wallet UK, Pixel 7, 2026-07-07)

The owner's device showed the parser's first guess was wrong, and it was
corrected to the real shape:

- **Title = merchant**, upper-cased: `SAINSBURYS`, `TESCO STORES 6530`.
- **Body = `£1.70 with Yonder ••0767`** — amount + card, **no verb and no inline
  merchant**. ("Yonder" is the card; `••0767` its last four.)

So `parsePayment` takes the merchant from the *title* and treats the `£… with …`
/ masked-card line as the purchase signal (there is no "paid"/"at" to key on).
Wallet posts these as a **group**, so the listener drops the group-summary parent
(`FLAG_GROUP_SUMMARY`) and logs only the per-transaction children. The card app
(Yonder) also notifies for the same purchase, but it is not in the allow-list, so
there is no double-logging.

## Consequences

- **Good:** Hands-free logging for the owner's actual signal (GPay), reusing the
  existing add + categorise pipeline; the parser is verifiable off-device
  (17 unit cases). Nothing leaves the phone; no schema migration; opt-in.
- **Bad:** Capture is not 100% — missed-while-unbound notifications and the
  version-dependent UK Wallet format mean some payments won't be caught, and the
  odd stray/mis-parsed entry may appear. The user reconciles manually.
- **Neutral / follow-ups (not in this PR):** a `requestRebind()` watchdog +
  battery-optimisation prompt to reduce misses; storing encrypted `sourceRaw`
  for re-parsing once crypto lands; tightening the regex against a corpus of the
  owner's real notifications. Notification access is a scrutinised Google Play
  capability — fine for the current sideloaded build, a review item before any
  Play release.

## Alternatives considered

- **Bank-app notifications instead of Google Pay** — often more reliable in the
  UK (instant, cleanly worded). Offered; the owner chose Google Pay. The
  listener is source-agnostic, so adding a bank package later is a one-line
  allow-list change.
- **A "pending review" inbox instead of silent auto-log** — rejected for now:
  the owner prefers entries to just appear and be edited, and the entries are
  already editable, so a separate confirmation queue adds friction without
  changing the correction path.
- **Read the SMS instead** — not applicable; the owner receives no bank SMS.
