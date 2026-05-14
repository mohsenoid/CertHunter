## 1. Domain helper

- [ ] 1.1 Add `AppCertificateDetails.toShareText(): String` as a pure extension or member function in `domain/model/`
- [ ] 1.2 Format includes SHA-256, SHA-1, owner, issuer, serial, validFrom, validUntil, and a validity marker
- [ ] 1.3 For `CertificateValidity.Expired` the formatted text includes an `EXPIRED` marker beside the validity dates
- [ ] 1.4 For `CertificateValidity.ExpiringSoon(daysLeft)` the formatted text includes an `EXPIRES IN N DAYS` marker

## 2. UI wiring

- [ ] 2.1 Add `R.string.share_certificate_button`, `R.string.share_certificate_chooser_title`, `R.string.share_certificate_marker_expired`, `R.string.share_certificate_marker_expiring` resources and translate them into every supported locale
- [ ] 2.2 Add `AppDetailAction.ShareCertificate` to the action sealed type
- [ ] 2.3 Add `AppDetailEvent.Share(text: String)` as a one-shot event in the ViewModel
- [ ] 2.4 In `AppDetailViewModel`, handle `ShareCertificate` by calling `toShareText()` on the displayed certificate and emitting `Share(text)`
- [ ] 2.5 Add a share `IconButton` to `AppDetailScreen` that dispatches `ShareCertificate`
- [ ] 2.6 In the root composable, collect events via `ObserveAsEvents` and dispatch `Intent.ACTION_SEND` wrapped in `Intent.createChooser` using `LocalContext`

## 3. Tests

- [ ] 3.1 Unit-test `toShareText()` for `Valid`, `ExpiringSoon(daysLeft)`, and `Expired` certificates
- [ ] 3.2 Unit-test `AppDetailViewModel` so that `ShareCertificate` emits a `Share` event whose text matches `toShareText()`
- [ ] 3.3 Confirm `./gradlew :app:detekt` and `./gradlew :app:testDebugUnitTest` pass

## 4. Manual verification

- [ ] 4.1 Build a debug APK, install on an API 24 device or emulator, tap share, send to a notes / email target, and confirm the formatted text is correct
- [ ] 4.2 Repeat on an API 34 device or emulator
- [ ] 4.3 Verify behaviour for a known expired certificate (e.g. install an app with an old cert) — confirm the `EXPIRED` marker appears in the shared text
