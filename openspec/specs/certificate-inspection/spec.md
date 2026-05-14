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

The system SHALL copy a certificate field to the system clipboard when the user taps that field. On Android API ≤ 12 (where the system does not show a clipboard notification), the system SHALL show a toast confirming the copy. On Android API ≥ 13 (where the system provides its own clipboard feedback) no toast is shown to avoid duplicate confirmation.

#### Scenario: Copy on Android 12

- **WHEN** the user taps the SHA-256 row on a device running API 32
- **THEN** the SHA-256 string is placed on the system clipboard
- **AND** a toast confirms the copy

#### Scenario: Copy on Android 13+

- **WHEN** the user taps the SHA-256 row on a device running API 33 or higher
- **THEN** the SHA-256 string is placed on the system clipboard
- **AND** no toast is shown (the system provides its own feedback)
