## ADDED Requirements

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
