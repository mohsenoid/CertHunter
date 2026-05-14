## 1. Domain helper

- [x] 1.1 Add `ShareCertificateLabels` data class in `domain/model/` carrying every user-visible label the formatter needs (field labels, validity-range label, status label, signer-header template, status markers)
- [x] 1.2 Add `AppDetails.toShareText(labels: ShareCertificateLabels): String` as a pure extension function in `domain/model/`
- [x] 1.3 Format: header is `appName` then `packageName` on the next line, blank line, then one signer block per entry in `AppDetails.certificates`; multiple signer blocks are separated by a blank line and a signer-header marker; historical certificates are excluded
- [x] 1.4 Each signer block contains rows for SHA-256, SHA-1, Owner, Issuer, Serial, the validity date range, and a Status line; labels are left-padded so values align in a column
- [x] 1.5 Status row maps `CertificateValidity.Valid` → `VALID`, `CertificateValidity.ExpiringSoon(daysLeft)` → `EXPIRES IN <daysLeft> DAYS`, and `CertificateValidity.Expired` → `EXPIRED`, taking the marker strings from `labels`

## 2. UI wiring

- [x] 2.1 Add `R.string.share_certificate_button`, `R.string.share_certificate_chooser_title`, `R.string.share_certificate_label_valid_range`, `R.string.share_certificate_label_status`, `R.string.share_certificate_signer_header`, `R.string.share_certificate_marker_valid`, `R.string.share_certificate_marker_expired`, and `R.string.share_certificate_marker_expiring` resources, and translate them into every supported locale
- [x] 2.2 Add a sealed `AppDetailAction` with a `ShareCertificate(labels: ShareCertificateLabels)` case
- [x] 2.3 Add a sealed `AppDetailEvent` with a `Share(text: String)` case exposed as a `SharedFlow` on `AppDetailViewModel`
- [x] 2.4 In `AppDetailViewModel`, retain the last successfully-loaded `AppDetails`, dispatch `AppDetailAction.ShareCertificate(labels)` by calling `details.toShareText(labels)` and emitting `AppDetailEvent.Share(text)`
- [x] 2.5 Add a share `IconButton` to `AppDetailScreen` that resolves the `ShareCertificateLabels` via `stringResource(...)` and dispatches `AppDetailAction.ShareCertificate(labels)`; the button is hidden while loading and when there are no active certificates
- [x] 2.6 In the detail root composable (in `AppNavHost`), observe `AppDetailViewModel.events` via an `ObserveAsEvents` helper and dispatch `Intent.ACTION_SEND` wrapped in `Intent.createChooser(...)` using `LocalContext`

## 3. Tests

- [x] 3.1 Unit-test `AppDetails.toShareText()` for `Valid`, `ExpiringSoon(daysLeft)`, and `Expired` certificates, asserting the byte-exact output against the canonical fixture in `design.md`
- [x] 3.2 Unit-test `AppDetails.toShareText()` for an app with two active signers, asserting both blocks are present and historical entries are absent
- [x] 3.3 Unit-test `AppDetailViewModel` so that dispatching `AppDetailAction.ShareCertificate(labels)` emits an `AppDetailEvent.Share` event whose text matches `details.toShareText(labels)`
- [x] 3.4 Confirm `./gradlew :app:detekt` and `./gradlew :app:testDebugUnitTest` pass

## 4. Manual verification

- [x] 4.1 Build a debug APK, install on an API 24 device or emulator, tap share, send to a notes / email target, and confirm the formatted text is correct
- [x] 4.2 Repeat on an API 34 device or emulator
- [x] 4.3 Verify behaviour for a known expired certificate (e.g. install an app with an old cert) — confirm the `EXPIRED` marker appears in the shared text
- [x] 4.4 Verify behaviour for a multi-signer app (if available) — confirm both signer blocks appear in the shared text and no historical entries are included
