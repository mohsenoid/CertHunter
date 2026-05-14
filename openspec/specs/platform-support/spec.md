# platform-support Specification

## Purpose

The platform-support capability defines the Android API surface CertHunter targets, the permission model it depends on, and the constraints those choices place on the rest of the codebase.

## Requirements

### Requirement: Supported Android versions

The system SHALL run on Android 7.0 (API 24) through Android 15 (API 37). `minSdk` SHALL be 24, `targetSdk` SHALL be 37, `compileSdk` SHALL be 37. Lowering `minSdk` below 24 is out of scope; raising `minSdk` requires a separate change proposal.

#### Scenario: Install on Android 7

- **WHEN** CertHunter is installed on a device running API 24
- **THEN** the app launches and all core flows (list, search, detail, copy) function
- **AND** API 28+ specific behaviour (signer rotation history) is gracefully unavailable

#### Scenario: Install on Android 15

- **WHEN** CertHunter is installed on a device running API 37
- **THEN** the app launches and renders Material You dynamic color
- **AND** active and historical signers are surfaced for apps that have rotated keys

### Requirement: Dual signing-API code paths

The system SHALL use two distinct `PackageManager` APIs to read signing certificates, selected at runtime by `Build.VERSION.SDK_INT`:

- API 28 and above: `GET_SIGNING_CERTIFICATES` together with `SigningInfo`.
- API 24 through 27: the deprecated `GET_SIGNATURES`.

Each path SHALL be exercised by unit tests in `AppRepositoryImplTest`. The branching logic SHALL live in `AppRepositoryImpl.getSignerBytes()` and nowhere else.

#### Scenario: API 28+ path

- **WHEN** the app runs on API 28 or higher
- **THEN** the repository calls `getPackageInfo(packageName, GET_SIGNING_CERTIFICATES)`
- **AND** the resulting `SigningInfo` is split into active and historical signers via `SignerSelector.select()`

#### Scenario: API 24–27 path

- **WHEN** the app runs on API 24, 25, 26, or 27
- **THEN** the repository calls `getPackageInfo(packageName, GET_SIGNATURES)`
- **AND** every returned `Signature` is treated as an active signer; no history is reported

### Requirement: Broad package visibility via QUERY_ALL_PACKAGES

The system SHALL declare the `QUERY_ALL_PACKAGES` permission in `AndroidManifest.xml`. Discovering installed apps *is* the core function — without broad package visibility the app cannot operate at all. The `<queries>` element is not a viable substitute because the app must enumerate every installed package, not a known fixed set.

The permission SHALL NOT be used for any secondary purpose: no package list is uploaded, shared, or stored outside the device.

#### Scenario: Manifest declaration

- **WHEN** the AndroidManifest is parsed at install time
- **THEN** `QUERY_ALL_PACKAGES` is declared as a `<uses-permission>` entry
- **AND** no `<queries>` element is needed to gate the package enumeration

#### Scenario: Play Store disclosure

- **WHEN** the app is submitted to Google Play
- **THEN** the QUERY_ALL_PACKAGES declaration is paired with a justification describing the developer-tool use case
- **AND** the project accepts the risk that Play may require appeal or rejection-response handling for this permission

### Requirement: Java time on API < 26

The system SHALL use `java.time.*` APIs for all date and validity computation. To make these APIs available on API levels 24 and 25, the build SHALL enable Android core library desugaring.

#### Scenario: Running on API 24

- **WHEN** the certificate validity check runs on a device with API 24
- **THEN** `ChronoUnit.DAYS.between(today, notAfter)` returns the correct day count via the desugared `java.time` runtime

### Requirement: No network or telemetry

The system SHALL NOT request `android.permission.INTERNET`. The app does not communicate with any network endpoint, does not emit telemetry, and does not persist a package list outside the device.

#### Scenario: Manifest audit

- **WHEN** the AndroidManifest is inspected
- **THEN** no `android.permission.INTERNET` entry is present
- **AND** no third-party analytics or crash-reporting SDK is included in the dependency graph
