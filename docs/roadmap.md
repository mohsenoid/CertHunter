# Roadmap

CertHunter follows a **Now / Next / Later** model. There are no fixed dates — priorities shift based on user feedback and real-world usage patterns.

---

## Now

**Play Store submission and first public release**

- Complete Google Play review and permission declaration approval (`QUERY_ALL_PACKAGES`)
  - ⚠️ Known risk: `QUERY_ALL_PACKAGES` is one of Play's most restricted permissions. CertHunter's developer-tool use case does not map to Google's typical approved categories (device search, antivirus, file manager, browser). There is no code-level workaround — the permission is fundamental to the app's purpose and cannot be replaced with `<queries>`. Mitigation: maintain a detailed Play Store disclosure; be prepared to appeal or respond to a rejection.
- First public release live on the Play Store
- Monitor early feedback and crash reports

---

## Next

The next set of features will be driven by what users actually ask for. The app is intentionally kept small until real usage patterns emerge. No features are locked in yet.

The one candidate already identified:

- **Export / share certificate** — allow sharing a certificate fingerprint or X.509 details via the Android share sheet, making it easier to paste into a service dashboard, send to a colleague, or drop into a bug report.

Everything else waits for signal from users.

---

## Later

Ideas worth exploring once there is enough feedback to know what matters most. None of these are committed.

### Export & share certificate details
Let users share a full certificate summary (SHA fingerprints, owner, issuer, validity) as plain text via the Android share sheet or copy to clipboard as a formatted block.

### Certificate change detection
Monitor selected apps and alert the user when a certificate changes — useful for catching unexpected key rotation after an app update, or for QA teams tracking signing identity across releases.

### Watchlist with backup / restore
Let users pin specific apps they care about (e.g. their own flavors) and persist that list so it survives reinstalls and device migrations.

### Search by fingerprint
Reverse lookup: paste a known SHA-1 or SHA-256 fingerprint and find which installed app matches it. Useful for debugging service integrations where you have the expected fingerprint but not the app.

### Compare two app flavors
Side-by-side view to compare the certificates of two installed apps — for example, the dev flavor vs. the production flavor installed on the same device.

---

## Guiding principle

CertHunter solves a specific pain point. Features that make it bigger but not sharper are a step in the wrong direction. Each addition should make the core use case — *instantly knowing the real certificate of an installed app* — faster or clearer.
