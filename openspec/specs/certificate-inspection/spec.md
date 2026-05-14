# certificate-inspection Specification

## Purpose

The certificate-inspection capability parses the signing certificate(s) of a single installed app, splits active signers from rotation history, computes validity state, and presents the fields in a detail view. It supports the user goal *"tell me what key actually signed this app and is that key still valid?"*.

## Requirements

### Requirement: Load app details by package name

The system SHALL load an app's certificate details by `packageName` via `AppRepository.getAppDetails(packageName: String)`. The repository SHALL return `Result<AppDetails, AppDetailsError>` (`com.michael-bull.kotlin-result`). The detail load SHALL run off the main thread on `DispatcherProvider.io`.

#### Scenario: Successful load

- **WHEN** the detail view is opened for an installed app whose certificate parses cleanly
- **THEN** the repository returns `Ok(AppDetails(...))` containing the `AppItem`, the active `certificates`, and the `historicalCertificates`
- **AND** the detail view renders all certificate fields

#### Scenario: App not found

- **WHEN** the requested `packageName` is not present on the device
- **THEN** the repository returns `Err(AppDetailsError.ItemLoadFailed(cause))`
- **AND** the detail view shows an error state instead of certificate fields

#### Scenario: Certificate cannot be parsed

- **WHEN** the signing bytes cannot be decoded as an `X509Certificate`
- **THEN** the repository returns `Err(AppDetailsError.CertificateParseFailed(cause))` or `Err(AppDetailsError.CertificateNotFound)`
- **AND** the detail view surfaces the failure without crashing the app

### Requirement: Split active and historical signers (API 28+)

On API 28 and above, the system SHALL use `PackageManager.GET_SIGNING_CERTIFICATES` and the resulting `SigningInfo` to distinguish *active* signers from rotation *history*. The split SHALL be performed by `SignerSelector.select()`, a pure function with the following behaviour:

- For a multi-signer APK (`SigningInfo.hasMultipleSigners() == true`): all entries from `apkContentsSigners` are active; the rotation history is ignored.
- For a single-signer APK: `apkContentsSigners[0]` is the sole active signer; `signingCertificateHistory` minus its last entry forms the historical set.

#### Scenario: Multi-signer APK

- **WHEN** an app is signed by multiple signers
- **THEN** every entry from `apkContentsSigners` is returned as active
- **AND** no historical signers are reported

#### Scenario: Single-signer APK with rotation history

- **WHEN** a single-signer app has rotated its signing key at least once
- **THEN** the most recent signer is reported as active
- **AND** all previous signers from `signingCertificateHistory` (excluding the active one) are reported as historical

### Requirement: Fallback signer extraction (API 24–27)

On API levels 24 through 27, where `GET_SIGNING_CERTIFICATES` is unavailable, the system SHALL fall back to the deprecated `PackageManager.GET_SIGNATURES`. All returned `Signature` entries SHALL be treated as active; no historical signers are reported on these API levels.

#### Scenario: Running on Android 7 (API 24)

- **WHEN** `Build.VERSION.SDK_INT` is less than 28
- **THEN** the repository uses `GET_SIGNATURES` to obtain signer bytes
- **AND** every returned signature appears in the active `certificates` list with an empty `historicalCertificates`

### Requirement: Parse X.509 fields

For each signer's bytes, the system SHALL parse a `java.security.cert.X509Certificate` and produce an `AppCertificateDetails` containing: SHA-256 fingerprint, SHA-1 fingerprint, subject (owner) DN, issuer DN, serial number, `validFrom`, `validUntil`, and a `CertificateValidity` state. Fingerprints SHALL be formatted as colon-separated uppercase hex.

#### Scenario: Standard certificate

- **WHEN** a valid X.509 certificate is parsed
- **THEN** the SHA-256 fingerprint is rendered as 32 uppercase hex pairs joined by `:`
- **AND** the SHA-1 fingerprint is rendered as 20 uppercase hex pairs joined by `:`
- **AND** the owner, issuer, and serial number fields are populated from the certificate

### Requirement: Compute validity state

The system SHALL classify each certificate's validity as one of three `CertificateValidity` states by comparing the certificate's `notAfter` to today's date:

- `Expired` — `notAfter` is in the past.
- `ExpiringSoon(daysLeft)` — `notAfter` is within 30 days of today.
- `Valid` — `notAfter` is more than 30 days in the future.

The 30-day threshold is a project-wide constant.

#### Scenario: Certificate expired yesterday

- **WHEN** the certificate's `notAfter` was one day ago
- **THEN** the validity is `Expired`
- **AND** the detail view shows an expired indicator

#### Scenario: Certificate expires in 15 days

- **WHEN** the certificate's `notAfter` is 15 days in the future
- **THEN** the validity is `ExpiringSoon` with `daysLeft = 15`
- **AND** the detail view shows an expiring-soon indicator alongside the days-left value

#### Scenario: Certificate valid for years

- **WHEN** the certificate's `notAfter` is more than 30 days in the future
- **THEN** the validity is `Valid`

### Requirement: Copy field to clipboard

The system SHALL copy a certificate field to the system clipboard when the user taps that field. On Android 12L and lower (API ≤ 32), where the system does not show a clipboard notification, the system SHALL show a toast confirming the copy. On Android 13 and higher (API ≥ 33), where the system provides its own clipboard feedback, no toast is shown to avoid duplicate confirmation.

#### Scenario: Copy on Android 12

- **WHEN** the user taps the SHA-256 row on a device running API 32
- **THEN** the SHA-256 string is placed on the system clipboard
- **AND** a toast confirms the copy

#### Scenario: Copy on Android 13+

- **WHEN** the user taps the SHA-256 row on a device running API 33 or higher
- **THEN** the SHA-256 string is placed on the system clipboard
- **AND** no toast is shown (the system provides its own feedback)

### Requirement: Share certificate as text

The system SHALL allow the user to share a formatted plain-text representation of the displayed app's active signers via the Android system share sheet. The shared text SHALL begin with a header containing the app name and package name, followed by one block per entry in `AppDetails.certificates`. Each signer block SHALL include the SHA-256 fingerprint, SHA-1 fingerprint, owner DN, issuer DN, serial number, the `validFrom`/`validUntil` date range, and a status marker line. Historical (rotated) certificates SHALL NOT be included in the shared text. The share action SHALL use `Intent.ACTION_SEND` with MIME type `text/plain`, wrapped in `Intent.createChooser`. No certificate data SHALL leave the device unless the user selects a target application from the chooser.

The status marker SHALL be one of three semantic cases — `Valid`, `ExpiringSoon(daysLeft)`, and `Expired` — rendered through localized string resources. The default (English) values are `VALID`, `EXPIRES IN <daysLeft> DAYS`, and `EXPIRED` respectively; each locale MAY adapt the wording, but the `ExpiringSoon` marker SHALL preserve the `daysLeft` substitution.

The formatted text SHALL be produced by a pure helper `AppDetails.toShareText(labels: ShareCertificateLabels)` in `domain/model/`, where `ShareCertificateLabels` is a data class carrying every user-visible label (field labels, signer header template, status markers). The helper SHALL NOT depend on the Android framework, so the formatting can be unit-tested in isolation.

#### Scenario: User taps share for a valid certificate

- **WHEN** the certificate detail view is open for an installed app whose active certificate is `Valid`
- **AND** the user taps the share `IconButton` in the detail dialog
- **THEN** the Android share sheet opens with `text/plain` content whose header contains the app name and package name
- **AND** the body contains the SHA-256, SHA-1, owner, issuer, serial, `validFrom`–`validUntil` range, and a status line rendering the localized `Valid` marker (default: `VALID`)
- **AND** the certificate data is not transmitted anywhere until the user selects a target app from the chooser

#### Scenario: User shares an expired certificate

- **WHEN** the displayed active certificate's validity is `CertificateValidity.Expired`
- **AND** the user taps share
- **THEN** the shared text's status line for that signer renders the localized `Expired` marker (default: `EXPIRED`)

#### Scenario: User shares a certificate expiring soon

- **WHEN** the displayed active certificate's validity is `CertificateValidity.ExpiringSoon(daysLeft)`
- **AND** the user taps share
- **THEN** the shared text's status line for that signer renders the localized `ExpiringSoon` marker with `daysLeft` substituted (default: `EXPIRES IN <daysLeft> DAYS`)

#### Scenario: User shares a multi-signer app

- **WHEN** the displayed app has more than one entry in `AppDetails.certificates`
- **AND** the user taps share
- **THEN** the shared text contains one signer block per active signer
- **AND** each signer block is preceded by a signer-header marker that identifies the signer's index
- **AND** no entry from `AppDetails.historicalCertificates` appears in the shared text
