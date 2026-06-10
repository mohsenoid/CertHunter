## ADDED Requirements

### Requirement: Validity classification uses a controlled current-date source

The system SHALL compute certificate validity (`Expired`, `ExpiringSoon(daysLeft)`, `Valid`) from a controlled current-date dependency supplied to the implementation, rather than by reading the ambient wall clock directly inside certificate parsing logic.

#### Scenario: Boundary case tested with a fixed current date

- **WHEN** the implementation evaluates certificate validity in unit tests
- **THEN** the current date can be fixed explicitly by the test
- **AND** the resulting validity classification is deterministic for expiry-boundary cases

#### Scenario: Certificate expires in exactly 30 days

- **WHEN** the controlled current date is 30 days before the certificate's `notAfter` date
- **THEN** the validity is `ExpiringSoon(30)`

#### Scenario: Certificate expires in 31 days

- **WHEN** the controlled current date is 31 days before the certificate's `notAfter` date
- **THEN** the validity is `Valid`
