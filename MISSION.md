# Mission

## The problem

When you work on multiple app flavors — dev, nightly, staging, production — getting the right signing certificate can become surprisingly painful. The keystore lives somewhere in CI. Or a cloud bucket nobody remembers. Or a release process signs the app at runtime and nobody can confidently tell you which key was actually used.

And even if someone hands you a keystore, one question still remains: **is this really the certificate of the app installed on the device?**

Developers waste real time hunting through build pipelines, secret stores, old docs, and tribal knowledge for a piece of information that is already sitting right there on the phone.

## The answer

CertHunter reads the signing certificate directly from apps already installed on the device — the only source that truly matters. No digging through CI. No asking around. No uncertainty about whether you are looking at the same key used in the actual build.

**The installed app is the source of truth.**

## Who it is for

Developers, release engineers, QA, and mobile teams who need to:

- Verify which certificate a specific app flavor is actually using
- Compare dev, nightly, staging, and production installs side by side on the same device
- Get SHA-1 and SHA-256 fingerprints instantly for service integrations — Google Pay, Google Sign-In, Firebase, Maps, or any third-party service that requires certificate verification
- Inspect X.509 details: owner, issuer, serial number, validity dates
- Detect expired or expiring certificates before they cause production incidents
- Audit key rotation history on apps that have rotated signing keys (API 28+)

## What it is not

CertHunter is a read-only, local inspection tool. It does not install, modify, or intercept anything. Everything is processed on the device. No data leaves the phone.

## Design principles

- **The device is the source of truth.** If the app is installed, its certificate is there. CertHunter surfaces it directly from `PackageManager` — no build system, no secrets, no guesswork.
- **Developer-grade detail.** Full certificate chain, historical signers, validity state, one-tap copy. Everything the platform exposes, presented clearly.
- **Local only.** No network requests, no telemetry, no accounts.
- **Broad compatibility.** Android 7.0 (API 24) through Android 15, handling both the legacy `GET_SIGNATURES` and the modern `GET_SIGNING_CERTIFICATES` + `SigningInfo` APIs transparently.

## Engineering standards

- Clean architecture: domain layer is pure Kotlin with no Android dependencies
- Every class in its own file; no god objects
- Unit-tested repository logic and ViewModel state with JUnit 5, Mockk, and Turbine
- Automated CI (tests + lint + build) on every PR; automated CD to Google Play internal testing on every release
