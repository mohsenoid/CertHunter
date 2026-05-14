## ADDED Requirements

### Requirement: Share certificate as text

The system SHALL allow the user to share a formatted plain-text representation of a displayed certificate via the Android system share sheet. The shared text SHALL include the SHA-256 fingerprint, SHA-1 fingerprint, owner DN, issuer DN, serial number, `validFrom`, `validUntil`, and a validity marker. The share action SHALL use `Intent.ACTION_SEND` with MIME type `text/plain`, wrapped in `Intent.createChooser`. No certificate data SHALL leave the device unless the user selects a target application from the chooser.

The formatted text SHALL be produced by a pure helper `AppCertificateDetails.toShareText()` so that the formatting can be unit-tested in isolation from the Android framework.

#### Scenario: User taps share for a valid certificate

- **WHEN** the certificate detail view is open for an installed app whose certificate is `Valid`
- **AND** the user taps the share `IconButton` in the detail dialog
- **THEN** the Android share sheet opens with `text/plain` content containing the SHA-256, SHA-1, owner, issuer, serial, `validFrom`, `validUntil`, and a `VALID` marker
- **AND** the certificate data is not transmitted anywhere until the user selects a target app from the chooser

#### Scenario: User shares an expired certificate

- **WHEN** the displayed certificate's validity is `CertificateValidity.Expired`
- **AND** the user taps share
- **THEN** the shared text includes an `EXPIRED` marker next to the `validUntil` value

#### Scenario: User shares a certificate expiring soon

- **WHEN** the displayed certificate's validity is `CertificateValidity.ExpiringSoon(daysLeft)`
- **AND** the user taps share
- **THEN** the shared text includes an `EXPIRES IN <daysLeft> DAYS` marker next to the `validUntil` value
