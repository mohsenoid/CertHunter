## 1. Inject a controlled time source

- [ ] 1.1 Add a `Clock` (or equivalent current-date provider) dependency to `AppRepositoryImpl`.
- [ ] 1.2 Wire the production dependency in `AppModule` using the system clock.

## 2. Refactor validity classification

- [ ] 2.1 Replace direct `LocalDate.now()` usage with the injected time source.
- [ ] 2.2 Keep the existing 30-day threshold and date formatting unchanged.
- [ ] 2.3 Extract validity classification into a small helper if that makes boundary testing materially clearer.

## 3. Add boundary tests

- [ ] 3.1 Add a test for expired yesterday -> `CertificateValidity.Expired`.
- [ ] 3.2 Add a test for expires today -> `CertificateValidity.ExpiringSoon(0)` or the explicitly chosen boundary result.
- [ ] 3.3 Add a test for expires in 30 days -> `CertificateValidity.ExpiringSoon(30)`.
- [ ] 3.4 Add a test for expires in 31 days -> `CertificateValidity.Valid`.

## 4. Validate and close out

- [ ] 4.1 Run `./gradlew :app:testDebugUnitTest`.
- [ ] 4.2 Run `openspec validate stabilize-certificate-time-handling --strict`.
- [ ] 4.3 Run `openspec archive stabilize-certificate-time-handling` after the implementation PR merges.
