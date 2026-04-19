# Mission

CertHunter exists to give Android developers and security researchers instant, on-device visibility into the signing certificates of any installed application — without sending data anywhere.

## What it does

CertHunter reads the X.509 signing certificates that Android's `PackageManager` exposes for every installed app and presents them in a clear, copyable format:

- **SHA-256 and SHA-1 fingerprints** — the values you compare against Google Play, Firebase, or an API allowlist
- **Full X.509 details** — owner, issuer, serial number, validity period
- **Active vs. historical signers** — surfaces the full signing history for apps that have rotated keys (API 28+)
- **Certificate validity state** — flags certificates that are expired or expiring soon

## Who it is for

- **Android developers** verifying their own app's signing configuration before release, or debugging certificate mismatches with Firebase, Google Maps, or backend services
- **Security researchers** auditing third-party APKs on a test device to confirm signing identity and detect certificate rotation or tampering

## What it is not

CertHunter is a read-only, local inspection tool. It does not install, modify, or intercept anything. No data leaves the device.

## Design principles

- **Local only.** All processing happens on the device using standard Android APIs. No network requests, no telemetry, no accounts.
- **Developer-grade detail.** Surface everything the platform exposes — not just fingerprints, but the full certificate chain, historical signers, and validity state — formatted so values can be copied in one tap.
- **Modern, minimal UI.** Material 3 + Jetpack Compose. Fast list with real-time search. Certificate details on tap. Nothing more.
- **Broad compatibility.** Supports Android 7.0 (API 24) through Android 15 (API 35), handling both the legacy `GET_SIGNATURES` API and the modern `GET_SIGNING_CERTIFICATES` + `SigningInfo` API transparently.

## Engineering standards

- Clean architecture: domain layer is pure Kotlin with no Android dependencies; data and UI layers are kept separate
- Every class in its own file; no god objects
- Unit-tested repository logic and ViewModel state with JUnit 5, Mockk, and Turbine
- Automated CI (tests + lint + build) on every PR; automated CD to Google Play internal testing on every release trigger
