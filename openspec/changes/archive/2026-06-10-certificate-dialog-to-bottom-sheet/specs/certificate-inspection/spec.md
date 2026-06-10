## MODIFIED Requirements

### Requirement: Load app details by package name

The system SHALL load an app's certificate details by `packageName` via `AppRepository.getAppDetails(packageName: String)`. The repository SHALL return `Result<AppDetails, AppDetailsError>` (`com.michael-bull.kotlin-result`). The detail load SHALL run off the main thread on `DispatcherProvider.io`.

#### Scenario: Successful load

- **WHEN** the bottom sheet is opened for an installed app whose certificate parses cleanly
- **THEN** the repository returns `Ok(AppDetails(...))` containing the `AppItem`, the active `certificates`, and the `historicalCertificates`
- **AND** the bottom sheet renders all certificate fields

#### Scenario: App not found

- **WHEN** the requested `packageName` is not present on the device
- **THEN** the repository returns `Err(AppDetailsError.ItemLoadFailed(cause))`
- **AND** the bottom sheet shows an error state instead of certificate fields

#### Scenario: Certificate cannot be parsed

- **WHEN** the signing bytes cannot be decoded as an `X509Certificate`
- **THEN** the repository returns `Err(AppDetailsError.CertificateParseFailed(cause))` or `Err(AppDetailsError.CertificateNotFound)`
- **AND** the bottom sheet surfaces the failure without crashing the app

### Requirement: Compute validity state

The system SHALL classify each certificate's validity as one of three `CertificateValidity` states by comparing the certificate's `notAfter` to today's date:

- `Expired` — `notAfter` is in the past.
- `ExpiringSoon(daysLeft)` — `notAfter` is within 30 days of today.
- `Valid` — `notAfter` is more than 30 days in the future.

The 30-day threshold is a project-wide constant.

#### Scenario: Certificate expired yesterday

- **WHEN** the certificate's `notAfter` was one day ago
- **THEN** the validity is `Expired`
- **AND** the bottom sheet shows an expired indicator

#### Scenario: Certificate expires in 15 days

- **WHEN** the certificate's `notAfter` is 15 days in the future
- **THEN** the validity is `ExpiringSoon` with `daysLeft = 15`
- **AND** the bottom sheet shows an expiring-soon indicator alongside the days-left value

#### Scenario: Certificate valid for years

- **WHEN** the certificate's `notAfter` is more than 30 days in the future
- **THEN** the validity is `Valid`

### Requirement: Share certificate as text

The system SHALL allow the user to share a formatted plain-text representation of the displayed app's active signers via the Android system share sheet. The shared text SHALL begin with a header containing the app name and package name, followed by one block per entry in `AppDetails.certificates`. Each signer block SHALL include the SHA-256 fingerprint, SHA-1 fingerprint, owner DN, issuer DN, serial number, the `validFrom`/`validUntil` date range, and a status marker line. Historical (rotated) certificates SHALL NOT be included in the shared text. The share action SHALL use `Intent.ACTION_SEND` with MIME type `text/plain`, wrapped in `Intent.createChooser`. No certificate data SHALL leave the device unless the user selects a target application from the chooser.

The status marker SHALL be one of three semantic cases — `Valid`, `ExpiringSoon(daysLeft)`, and `Expired` — rendered through localized string resources. The default (English) values are `VALID`, `EXPIRES IN <daysLeft> DAYS`, and `EXPIRED` respectively; each locale MAY adapt the wording, but the `ExpiringSoon` marker SHALL preserve the `daysLeft` substitution.

The formatted text SHALL be produced by a pure helper `AppDetails.toShareText(labels: ShareCertificateLabels)` in `domain/model/`, where `ShareCertificateLabels` is a data class carrying every user-visible label (field labels, signer header template, status markers). The helper SHALL NOT depend on the Android framework, so the formatting can be unit-tested in isolation.

#### Scenario: User taps share for a valid certificate

- **WHEN** the certificate bottom sheet is open for an installed app whose active certificate is `Valid`
- **AND** the user taps the share `IconButton` in the bottom sheet
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

### Requirement: Detail-sheet local UI state is package-scoped

Local UI state owned by the certificate detail bottom sheet, such as whether the historical-certificates section is expanded, SHALL belong to the currently displayed package only. The history section SHALL always open in the default collapsed state, regardless of prior interaction with the same or a different app. Opening a different app's detail bottom sheet SHALL NOT restore local expansion state from a previous app.

#### Scenario: History expansion does not leak across apps

- **WHEN** the user opens app A's detail bottom sheet and expands the historical certificates section
- **AND** dismisses that bottom sheet
- **AND** later opens app B's detail bottom sheet
- **THEN** app B's historical certificates section starts from its default collapsed state
- **AND** app A's previous expansion state does not leak into app B's bottom sheet

#### Scenario: History expansion resets on re-open for same app

- **WHEN** the user opens app A's detail bottom sheet and expands the historical certificates section
- **AND** dismisses that bottom sheet
- **AND** re-opens app A's detail bottom sheet
- **THEN** app A's historical certificates section starts from its default collapsed state

## ADDED Requirements

### Requirement: Dismiss bottom sheet

The certificate detail bottom sheet SHALL support two dismissal gestures: swipe-to-dismiss (dragging the sheet downward past a threshold) and scrim-tap (tapping the dimmed area behind the sheet). Both gestures SHALL dismiss the sheet with its hide animation before removing the back-stack entry.

#### Scenario: User swipes sheet down to dismiss

- **WHEN** the certificate detail bottom sheet is open
- **AND** the user drags the sheet downward past the dismiss threshold
- **THEN** the sheet plays its hide animation
- **AND** the certificate bottom sheet is removed from the screen

#### Scenario: User taps scrim to dismiss

- **WHEN** the certificate detail bottom sheet is open
- **AND** the user taps the dimmed area behind the sheet
- **THEN** the sheet plays its hide animation
- **AND** the certificate bottom sheet is removed from the screen

#### Scenario: User taps close button to dismiss

- **WHEN** the certificate detail bottom sheet is open
- **AND** the user taps the close `IconButton` in the sheet header
- **THEN** the sheet plays its hide animation
- **AND** the certificate bottom sheet is removed from the screen
