## Why

Certificate validity classification currently depends on ambient wall-clock state inside `AppRepositoryImpl.parseCertificate()` via `LocalDate.now()` and `ZoneId.systemDefault()`. That makes boundary behaviour harder to reason about and harder to test:

1. The "today" value is not controlled by tests, so exact expiry-boundary cases are only indirectly covered.
2. Date rollover and timezone differences can change the computed `daysLeft` around midnight without any explicit dependency in the code.
3. The repository combines certificate parsing, date conversion, and validity classification in one method, making deterministic tests more awkward than they need to be.

The app's certificate-inspection behaviour depends on those boundaries being stable. This change makes the time dependency explicit and adds specification coverage for the boundary cases.

## What Changes

- Introduce an injected time source for certificate validity evaluation, such as `java.time.Clock` or an equivalent current-date provider.
- Refactor certificate parsing so validity classification reads "today" from that injected source instead of calling `LocalDate.now()` directly.
- Keep the existing 30-day validity semantics and visible formatting unchanged.
- Add targeted tests for boundary cases like expired yesterday, expires today, expires in 30 days, and expires in 31 days.
- Add a `certificate-inspection` spec delta clarifying that validity classification is computed from a controlled current-date dependency rather than ambient wall-clock access.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `certificate-inspection`: clarifies deterministic validity classification rules and the controlled source of "today".

## Impact

- **Code**:
  - `app/src/main/java/com/mohsenoid/certhunter/data/repository/AppRepositoryImpl.kt`
  - possible new helper under `app/src/main/java/com/mohsenoid/certhunter/data/repository/` or `domain/model/`
  - `app/src/main/java/com/mohsenoid/certhunter/di/AppModule.kt`
- **Tests**:
  - `app/src/test/java/com/mohsenoid/certhunter/data/repository/AppRepositoryImplTest.kt`
  - any new focused unit test for validity classification if the logic is extracted
- **Specs**:
  - delta to `openspec/specs/certificate-inspection/spec.md`
- **APIs / dependencies**: no new external dependency; uses `java.time` primitives already in the project.
- **Runtime behaviour**: intended to remain the same for ordinary users, while becoming deterministic and explicitly testable at date boundaries.
