## Why

Users have asked to share a certificate fingerprint or X.509 summary out of CertHunter — to paste into a service dashboard, attach to a bug, or send to a colleague who is configuring a Google Sign-In / Firebase / Maps integration. Today the only option is per-field clipboard copy, which is tedious when several fields are needed and impossible to use from a separate device. This is the only roadmap item currently identified for "Next".

## What Changes

- Add a share action to the certificate detail view that opens the Android system share sheet with a formatted plain-text summary of the displayed app's active signers.
- The summary text MUST include a header with the app name and package name, followed by one block per active signer containing the SHA-256 fingerprint, SHA-1 fingerprint, owner DN, issuer DN, serial number, the validity date range, and a status marker (`VALID`, `EXPIRES IN <n> DAYS`, or `EXPIRED`).
- Historical (rotated) certificates are excluded from the shared text.
- The share uses `Intent.ACTION_SEND` with MIME type `text/plain`. No certificate data leaves the device unless the user selects a target app.
- Out of scope: sharing as image / PDF, sharing the full DER or PEM blob, sharing rotation history, bulk-sharing multiple apps in one action.

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `certificate-inspection`: gains a "Share certificate as text" requirement that defines the share action's behaviour, the formatted text contents (including multi-signer composition and status markers), and the scope of what is shared.

## Impact

- Code: new pure helper `AppDetails.toShareText(labels)` in `domain/model/` with a companion `ShareCertificateLabels` data class; share action / event added to `AppDetailViewModel`, `AppDetailAction`, `AppDetailEvent`; share `IconButton` added to `AppDetailScreen`; `Intent.ACTION_SEND` dispatch wired in the root composable via `LocalContext`.
- Resources: new `R.string.share_certificate_*` entries for the button label, share-sheet title, status markers (`VALID` / `EXPIRED` / `EXPIRES IN %d DAYS`), the signer-header marker, and the `Valid` / `Status` row labels. Reuses existing `app_detail_label_*` strings for SHA-256, SHA-1, Owner, Issuer, and Serial. Translated alongside the existing strings.
- Tests: unit tests for `toShareText()` covering valid / expiring / expired states and the multi-signer case; ViewModel test for the share action emitting the expected event.
- Permissions / SDKs: no new permissions, no new dependencies. Uses `Intent.ACTION_SEND` and `Intent.createChooser`, both available on minSdk 24.
- Risk: the share sheet is system-rendered and untestable in unit tests; manual verification is required.
