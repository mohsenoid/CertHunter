## Context

`AppRepositoryImpl.parseCertificate()` currently performs three responsibilities together:

1. Decode raw bytes into `X509Certificate`
2. Convert validity dates into local `yyyy-MM-dd` strings
3. Classify validity with `LocalDate.now()` and a fixed 30-day threshold

That is workable, but the direct `LocalDate.now()` call makes it impossible to drive the exact boundary date from tests without relying on the machine clock. CertHunter's `certificate-inspection` spec already defines exact semantics for `Expired`, `ExpiringSoon(daysLeft)`, and `Valid`, so the implementation should expose a deterministic time dependency.

## Goals / Non-Goals

**Goals:**

- Make the source of "today" explicit and injectable.
- Preserve the current user-facing date formatting and 30-day threshold.
- Add exact boundary tests for validity classification.

**Non-Goals:**

- Changing how certificate dates are displayed to the user.
- Changing the 30-day threshold.
- Redesigning the repository API shape unless the extraction clearly simplifies the implementation.

## Decisions

### Decision 1: Inject a time source into the repository layer

`AppRepositoryImpl` will receive a controlled time source, preferably `java.time.Clock`, via constructor injection. Validity classification will derive `LocalDate.now(clock)` rather than `LocalDate.now()`.

**Rationale.** `Clock` is a standard JDK abstraction, fits the existing `java.time` code, and is easy to fix in tests.

**Alternatives considered.**

- Inject `LocalDate` directly. Rejected: too narrow and awkward if more date/time usage appears later.
- Hide time behind a custom interface. Rejected for now: more project-specific abstraction than the current need justifies.

### Decision 2: Keep date rendering local-time-based, but separate it from "today"

Certificate `notBefore` / `notAfter` values will continue to be converted through the current zone for user-facing `validFrom` / `validUntil` strings. The injected clock controls the current date used for classification, not the certificate's own encoded timestamps.

**Rationale.** The user-visible strings should still match local expectations. The instability problem is the uncontrolled current date, not the presence of local formatting itself.

### Decision 3: Add boundary-driven tests at the repository or extracted-helper level

Tests will cover:

- expired yesterday → `Expired`
- expires today → `ExpiringSoon(0)` unless the implementation intentionally normalizes differently
- expires in 30 days → `ExpiringSoon(30)`
- expires in 31 days → `Valid`

If extracting the validity classification into a helper materially simplifies these tests, that extraction is acceptable within this change.

## Risks / Trade-offs

- **Risk:** Injecting `Clock` into `AppRepositoryImpl` slightly expands constructor wiring.
  **Mitigation:** `AppModule` can supply `Clock.systemDefaultZone()` in one line.

- **Risk:** Existing tests that implicitly rely on the machine clock may become brittle when updated.
  **Mitigation:** Replace them with fixed-clock assertions rather than broad "field is populated" checks where appropriate.

- **Trade-off:** This change does not fully purify certificate parsing because local date rendering still depends on zone rules.
  **Why acceptable:** The main defect risk is nondeterministic validity classification. Full parser extraction can wait until there is a second reason to do it.

## Migration Plan

No data migration. Internal-only refactor with additional tests.

## Open Questions

None.
