## Why

Users have asked to share a certificate fingerprint or X.509 summary out of CertHunter — to paste into a service dashboard, attach to a bug, or send to a colleague who is configuring a Google Sign-In / Firebase / Maps integration. Today the only option is per-field clipboard copy, which is tedious when several fields are needed and impossible to use from a separate device. This is the only roadmap item currently identified for "Next".

## What Changes

- Add a share action to the certificate detail view that opens the Android system share sheet with a formatted plain-text summary of the displayed certificate.
- The summary text MUST include the SHA-256 fingerprint, SHA-1 fingerprint, owner DN, issuer DN, serial number, and validity (with an explicit marker for an expired or expiring certificate).
- The share uses `Intent.ACTION_SEND` with MIME type `text/plain`. No certificate data leaves the device unless the user selects a target app.
- Out of scope: sharing as image / PDF, sharing the full DER or PEM blob, bulk-sharing multiple apps in one action.

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `certificate-inspection`: gains a "Share certificate as text" requirement that defines the share action's behaviour, the formatted text contents, and the expired-marker behaviour.

## Impact

- Code: new pure helper `AppCertificateDetails.toShareText()` in `domain/model/`; share action / event added to `AppDetailViewModel`, `AppDetailAction`, `AppDetailEvent`; share `IconButton` added to `AppDetailScreen`; `Intent.ACTION_SEND` dispatch wired in the root composable via `LocalContext`.
- Resources: new `R.string.share_certificate_*` entries for the button label, share-sheet title, and any in-text labels (e.g. "EXPIRED" marker), translated alongside the existing strings.
- Tests: unit tests for `toShareText()` covering valid / expiring / expired states; ViewModel test for the share action emitting the expected event.
- Permissions / SDKs: no new permissions, no new dependencies. Uses `Intent.ACTION_SEND` and `Intent.createChooser`, both available on minSdk 24.
- Risk: the share sheet is system-rendered and untestable in unit tests; manual verification is required.
