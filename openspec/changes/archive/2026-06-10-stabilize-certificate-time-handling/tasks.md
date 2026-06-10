## 1. Inject a controlled time source

- [x] 1.1 Add a `Clock` (or equivalent current-date provider) dependency to `AppRepositoryImpl`.
- [x] 1.2 Wire the production dependency in `AppModule` using the system clock.

## 2. Refactor validity classification

- [x] 2.1 Replace direct `LocalDate.now()` usage with the injected time source.
- [x] 2.2 Keep the existing 30-day threshold and date formatting unchanged.
- [x] 2.3 Extract validity classification into a small helper if that makes boundary testing materially clearer.

## 3. Add boundary tests

- [x] 3.1 Add a test for expired yesterday -> `CertificateValidity.Expired`.
- [x] 3.2 Add a test for expires today -> `CertificateValidity.ExpiringSoon(0)` or the explicitly chosen boundary result.
- [x] 3.3 Add a test for expires in 30 days -> `CertificateValidity.ExpiringSoon(30)`.
- [x] 3.4 Add a test for expires in 31 days -> `CertificateValidity.Valid`.

## 4. Validate and close out

- [x] 4.1 Run `./gradlew :app:testDebugUnitTest`.
- [x] 4.2 Run `openspec validate stabilize-certificate-time-handling --strict`.
- [ ] 4.3 Run `openspec archive stabilize-certificate-time-handling` after the implementation PR merges.
