## Context

The certificate detail dialog already renders 1..N active signers and 0..M historical certificates per app. Today, fields can only be copied one at a time, which is slow when a colleague configuring Firebase / Google Sign-In / Maps needs SHA-256, SHA-1, package name, and identity together. The proposal calls for a share action that emits a plain-text summary via `Intent.ACTION_SEND`.

Three artifacts already exist:
- `proposal.md` — scope, intent, impact
- `specs/certificate-inspection/spec.md` — three scenarios (valid / expired / expiring soon)
- `tasks.md` — helper, UI wiring, tests, manual verification

The proposal speaks of "the displayed certificate" (singular), but the screen routinely shows multiple active signers and an expandable history. This document pins down the design questions that singular phrasing leaves open.

Constraints inherited from the project:
- minSdk 24, so the share path must work on API 24–27 (`GET_SIGNATURES`) and API 28+ (`SigningInfo`) data alike
- Helper must be a pure function in `domain/model/` so it is unit-testable without the Android framework
- Every class lives in its own file
- No new dependencies

## Goals / Non-Goals

**Goals:**
- Share a single plain-text summary that covers everything visible in the active-signer section of the detail dialog
- Include app identity (name + package) in the shared text so the recipient knows what the fingerprint belongs to
- Produce a deterministic format with a canonical fixture, so the unit test is not a whitespace debate
- Preserve the on-screen fingerprint formatting (colon-separated hex) so paste matches what the user saw
- Always include a validity tag — `VALID`, `EXPIRES IN <n> DAYS`, or `EXPIRED` — for consistency

**Non-Goals:**
- Sharing the historical-certificate list (auxiliary information, hidden behind an expand toggle)
- Per-signer share buttons (would clutter the dialog with 1+N+M icons for marginal benefit)
- Alternate formats: PEM/DER, image, QR code, "raw" colon-stripped fingerprints
- Bulk-sharing multiple apps in one action
- A `design.md`-level decision about the share-sheet's target apps; that is system-rendered

## Decisions

### D1. One share button in the dialog header; emits all active signers

The share `IconButton` lives in the `AlertDialog` title row (or as part of the confirm-button row, alongside Close). One press shares a single string containing the app identity header plus every entry in `AppDetails.certificates`.

**Alternatives considered:**
- *Per-signer share button* — too cluttered; multi-signer apps are a long tail (key-rotation overlap), and history would also demand its own button if we went down this road.
- *Active-signer only, button only visible for single-signer apps* — surprising UX; users who actually see multi-signer apps are the ones most likely to need to share both.

**Implication:** the proposal's "displayed certificate" phrasing (singular) needs to widen to "all displayed active signers plus app identity." See [Implications](#implications-for-proposal--spec--tasks).

### D2. Helper lives on `AppDetails`, not `AppCertificateDetails`

Signature: `fun AppDetails.toShareText(labels: ShareCertificateLabels): String` (extension function in `domain/model/`, where `ShareCertificateLabels` is a framework-free carrier for every user-visible label — field labels, signer header template, status markers).

`AppCertificateDetails` does not know its own app name or package — those are on `AppItem`. Since the shared text needs both identity and (one or more) certificates, the natural carrier is `AppDetails`. Keeping the helper at the `AppDetails` level also means we own multi-signer composition in one place rather than re-implementing it at the call site.

Labels are injected through `ShareCertificateLabels` rather than read from resources inside the helper so the function stays pure and Android-framework-free for unit testing; the call site resolves `stringResource(...)` values and hands them in.

**Alternatives considered:**
- `AppCertificateDetails.toShareText()` taking `appName: String, packageName: String` parameters — works, but the caller has to loop and concatenate for multi-signer apps; the multi-signer formatting logic ends up split between the helper and the ViewModel.

### D3. Format: compact, labeled, paste-friendly

Canonical fixture (used verbatim as the test expectation for the single-signer Valid case):

```
CertHunter
com.mohsenoid.certhunter

SHA-256: A1:B2:C3:D4:E5:F6:A1:B2:C3:D4:E5:F6:A1:B2:C3:D4
SHA-1:   A1:B2:C3:D4:E5:F6:A1:B2:C3:D4
Owner:   CN=Example, O=Example Corp, C=US
Issuer:  CN=Example CA, O=Example Corp, C=US
Serial:  123456789
Valid:   2023-01-01 → 2033-01-01
Status:  VALID
```

Rules:
- Header is two lines: app name on line 1, package on line 2, blank line after
- One blank line between header and the first signer block
- Each signer block: `SHA-256`, `SHA-1`, `Owner`, `Issuer`, `Serial`, `Valid` (range), `Status`, in that order
- Labels are left-padded so values align (the colon column is fixed)
- Multi-signer apps: each additional signer is prefixed by a blank line and the marker line `--- Signer N ---`
- Fingerprints keep their on-screen colon-separated form
- The validity range uses the same `validFrom` / `validUntil` strings the screen displays — no re-formatting

### D4. Status line is always present; expired / expiring soon adopt distinct markers

`Status:` value mapping:
- `CertificateValidity.Valid` → `VALID`
- `CertificateValidity.ExpiringSoon(daysLeft)` → `EXPIRES IN <daysLeft> DAYS`
- `CertificateValidity.Expired` → `EXPIRED`

`VALID` is included for symmetry, not as a warning. Consistency is worth more than terseness here — the recipient always knows where to look.

The marker strings are surfaced through string resources (`R.string.share_certificate_marker_valid`, `_expired`, `_expiring`) so translators can adapt them. Tasks 2.1 already covers expired and expiring; add a `_valid` resource.

### D5. Fingerprint colons preserved

The shared text mirrors what the user sees on the screen. Firebase, Google Cloud Console, and most keytool flows accept colon-separated fingerprints, so this is the safer default. A future "raw" / colon-stripped mode could be added behind an explicit toggle, but it is not in scope.

### D6. History is excluded from the shared text

Historical certificates are hidden behind an expand toggle on screen — they are auxiliary, not part of the primary "what is this app signed with right now" answer. Sharing the full signing history would also make the text long, and the primary use case (paste fingerprint into Firebase) only ever wants the current signers.

If someone wants to share a historical entry, copying individual fields still works.

## Risks / Trade-offs

- **Manual verification is the only end-to-end test** → the share sheet is system-rendered and untestable in unit tests; mitigated by manual verification on API 24 and a recent API in tasks 4.1–4.3, plus a deterministic test of `toShareText()` that locks the exact output bytes.
- **Localized labels in the body of the shared text** → labels (`SHA-256`, `Owner`, etc.) are technical identifiers; the recipient may be in a different locale. Mitigation: keep the field labels in `R.string.share_certificate_*` so each locale can decide whether to translate them; the canonical fixture uses the en-US strings.
- **Padding-based alignment depends on labels of similar length** → if a future label is much longer ("Public Key Algorithm:" etc.) the alignment may need to shift to a single space. Mitigation: not a problem today; revisit if/when more fields are added.
- **Status line at the bottom may be missed by recipients skimming** → mitigation: the EXPIRED / EXPIRES IN N DAYS marker is a separate visible line, not appended to the date; if real-world users miss it we can revisit (e.g., move the marker to the header).
- **Multi-signer apps emit a longer blob** → typically 1 signer; the long-tail case (dual-signing during key rotation) produces ~2x output. Acceptable.

## Implications for proposal / spec / tasks

Locking these decisions in means the existing artifacts need narrow updates. None of these change the user-facing intent of the change, only its phrasing and helper signature:

**`proposal.md`:**
- "The summary text MUST include..." → keep the field list, but add app name and package name as required header fields
- "the displayed certificate" → "all active signers visible in the detail dialog, with app identity in the header"
- Impact section: helper signature is `AppDetails.toShareText(labels: ShareCertificateLabels)`, not `AppCertificateDetails.toShareText()`

**`specs/certificate-inspection/spec.md`:**
- Requirement body: add app name and package name to the required content list; clarify that the text covers all active signers (not history)
- "Valid" scenario: rename the implicit `VALID` marker requirement so it sits alongside expired/expiring as one of three explicit values
- Add a fourth scenario for the multi-signer case (e.g., "When the active signer list contains 2 entries, the shared text contains both, separated by a signer-N marker")

**`tasks.md`:**
- 1.1: helper is `AppDetails.toShareText(labels: ShareCertificateLabels)`, not on `AppCertificateDetails`
- 1.2: include app name + package in the format spec
- Add a `R.string.share_certificate_marker_valid` resource to 2.1
- Add a multi-signer test to section 3 alongside the validity-state tests
- Adjust 2.4: ViewModel calls `appDetails.toShareText()` directly (no per-cert plumbing)

## Open Questions

- Should the share button be hidden when `uiState.certificates` is empty (no signature found)? Lean: yes — nothing to share. Not currently captured anywhere.
- Should the share text include `firstInstallTime` from `AppItem`? Lean: no — recipients configuring Firebase don't need it, and the screen doesn't show it. Out of scope unless a real user asks.
- If a future change adds the public key algorithm or SAN list to the detail screen, do they belong in the share text? Defer until the screen changes.
