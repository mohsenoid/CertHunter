# app-inventory Specification

## Purpose

The app-inventory capability enumerates all apps installed on the device, presents them in a searchable, sortable list, and lets the user open any app's certificate detail view. It is the entry point of CertHunter and the only screen the user sees on launch.

## Requirements

### Requirement: Enumerate installed apps

The system SHALL list every app installed on the device — both user-installed and system apps — by querying `PackageManager`. The query SHALL be performed off the main thread on `DispatcherProvider.io`. Each app entry SHALL expose its display name, `packageName`, `isSystemApp` flag, and `firstInstallTime`.

#### Scenario: First launch

- **WHEN** the user opens CertHunter for the first time after install
- **THEN** the app list screen displays a loading indicator until `PackageManager` returns
- **AND** every installed app appears as a row showing its icon, label, and package name

#### Scenario: System app inclusion

- **WHEN** the system app filter is enabled
- **THEN** apps with the `ApplicationInfo.FLAG_SYSTEM` flag appear in the list
- **AND** apps without that flag are omitted when the user has chosen "user apps only"

### Requirement: Surface load errors with retry

The system MUST NOT crash if `PackageManager` throws. The ViewModel SHALL catch the exception, expose a `hasLoadError` flag in its UI state, and offer the user a retry action that re-runs the query.

#### Scenario: Initial load fails

- **WHEN** `PackageManager.getInstalledPackages` throws on the initial load
- **THEN** the app list screen shows an error widget with a "Retry" button
- **AND** tapping "Retry" clears the error state and re-runs the query

#### Scenario: Pull-to-refresh fails

- **WHEN** the user pulls to refresh and the underlying query throws
- **THEN** the previously loaded list remains visible
- **AND** the error widget is shown alongside the existing data, not in place of it

### Requirement: Search by name or package

The system SHALL filter the displayed list by a case-insensitive substring match against either the app's display name or its `packageName`. Filtering SHALL be computed in the UI model layer as a derived property — never stored as separate state.

#### Scenario: Substring filter

- **WHEN** the user types "chrome" into the search field
- **THEN** the list shows only apps whose display name or package name contains "chrome" (case-insensitive)

#### Scenario: Empty query

- **WHEN** the search field is cleared
- **THEN** the unfiltered list is shown, preserving the current sort order

### Requirement: Sort by name or install date

The system SHALL allow the user to sort the list by one of four orders: name ascending, name descending, install date newest first, install date oldest first. The chosen sort SHALL persist for the duration of the session.

#### Scenario: Sort by newest install

- **WHEN** the user selects "Install date — newest first"
- **THEN** the list reorders so that the most recently installed app appears at the top
- **AND** the active sort order is reflected in the sort control

### Requirement: Open certificate detail on tap

The system SHALL navigate to the certificate detail view for an app when the user taps its row. Navigation SHALL pass the app's `packageName` as a type-safe route argument.

#### Scenario: User taps a row

- **WHEN** the user taps any app row in the list
- **THEN** the app detail destination is pushed onto the back stack with the tapped app's `packageName`
- **AND** the back stack returns to the list when the detail view is dismissed

### Requirement: Off-main-thread icon loading

The system SHALL load app icons off the main thread via a custom Coil `Fetcher<AppIconData>` registered globally at application startup. The main thread MUST NOT block on `PackageManager.getApplicationIcon`.

#### Scenario: Scrolling the list

- **WHEN** the user scrolls quickly through a long list
- **THEN** rows render without blocking the main thread for icon decoding
- **AND** icons appear asynchronously as Coil resolves them through `AppIconFetcher`

### Requirement: Newest app-list request wins

When multiple app-list loads overlap, the system SHALL treat the newest request as authoritative. Results from an older request SHALL NOT overwrite UI state that already reflects a newer request's outcome.

#### Scenario: Retry supersedes an older load

- **WHEN** an initial app-list load is still in flight
- **AND** the user triggers a newer retry or refresh request
- **THEN** only the newer request may update the visible app list and loading/error flags
- **AND** a late result from the older request is ignored or cancelled before it reaches the UI state

### Requirement: Refresh retry clears stale refresh error state

Starting a new refresh after a failed refresh SHALL clear the previous refresh-error presentation immediately, while keeping the existing app list visible during the new request.

#### Scenario: User retries after refresh failure

- **WHEN** the user previously saw the refresh error banner
- **AND** they start a new pull-to-refresh
- **THEN** the old refresh error banner is cleared as soon as the new refresh begins
- **AND** the existing app list remains visible until the new refresh completes
