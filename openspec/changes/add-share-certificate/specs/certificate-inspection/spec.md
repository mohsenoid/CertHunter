## ADDED Requirements

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
