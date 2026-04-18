# Release Automation Design — Continuous Distribution to Google Play Store

**Date:** 2026-04-18
**Status:** Approved

## Problem

CertHunter's release workflow builds a signed APK and creates a GitHub Release when a `v*` tag is pushed, but there is no automated distribution to the Google Play Store. Each release requires manual Play Store upload, version management, and GitHub Release creation.

## Goals

- Automate `versionCode` / `versionName` calculation from the semver tag (no manual edits before tagging)
- Build both AAB (Play Store) and APK (sideloading)
- Upload the AAB as a draft to the Play Store internal testing track automatically on tag push
- Attach both AAB and APK to the GitHub Release
- Keep `main` in sync with the shipped version via a commit-back

## Non-Goals

- Managing Play Store store listing metadata, screenshots, or release notes via CI
- Automatically promoting from internal to alpha/beta/production (stays manual in Play Console)

## Architecture

### Trigger

Tag push matching `v*` (e.g., `v1.3.0`) triggers the workflow. A `workflow_dispatch` input allows manual test runs without side effects.

### Pipeline Steps

```
Tag v1.3.0 pushed
       │
       ▼
1. Checkout (full history)
2. Resolve version from tag → versionName="1.3.0", versionCode=1003000
3. Patch app/build.gradle.kts (local only — build uses this)
4. Set up JDK 21 + Gradle
5. Decode keystore from secret
6. ./gradlew bundleRelease assembleRelease  (signed)
7. Upload to Play Store internal track (draft)
8. Create GitHub Release with AAB + APK
9. Commit-back to main (only if all prior steps succeed)
```

### versionCode Formula

```
versionCode = MAJOR × 1,000,000 + MINOR × 1,000 + PATCH
```

| Tag | versionName | versionCode |
|-----|------------|-------------|
| v1.2.0 | 1.2.0 | 1002000 |
| v1.3.0 | 1.3.0 | 1003000 |
| v1.2.1 | 1.2.1 | 1002001 |
| v2.0.0 | 2.0.0 | 2000000 |

Hotfixes always have a lower versionCode than their successor minor release, preserving correct semantic ordering. Supports up to MAJOR=999, MINOR=999, PATCH=999.

### Version Commit-Back

After all build and release steps succeed, the workflow commits back to `main` using `GITHUB_TOKEN` with `contents: write` permission. The commit message includes `[skip ci]` as a conventional marker, but this has no functional effect: the CI workflow (`ci.yml`) only triggers on `pull_request` and `workflow_dispatch`, so a direct push to `main` never runs CI regardless.

Guard: `git diff --cached --quiet || git commit ...` prevents an empty commit if the tag was pushed after a manual version bump.

### Play Store Upload

Tool: `r0adkll/upload-google-play@v1`
- Package: `com.mohsenoid.certhunter`
- Track: `internal`
- Status: `draft` (requires human publish click in Play Console before testers see it)

Only runs on real tag pushes (not `workflow_dispatch`).

### GitHub Release

Uses `softprops/action-gh-release@v2`. Attaches both:
- `app/build/outputs/bundle/release/*.aab` — for Play Store / reference
- `app/build/outputs/apk/release/*.apk` — for sideloading

## Secrets

| Secret | Status | Description |
|--------|--------|-------------|
| `KEYSTORE_BASE64` | Existing | Base64-encoded release keystore |
| `KEYSTORE_PASSWORD` | Existing | Keystore password |
| `RELEASE_KEY_ALIAS` | Existing | Release key alias |
| `RELEASE_KEY_PASSWORD` | Existing | Release key password |
| `PLAY_STORE_JSON_KEY` | **New** | Google Play service account JSON (plain text) |

### Creating `PLAY_STORE_JSON_KEY`

1. Google Play Console → Setup → API access
2. Link or create a Google Cloud project
3. Create a service account
4. Grant "Release manager" role in Play Console
5. Create and download a JSON key file
6. Add the full JSON content as `PLAY_STORE_JSON_KEY` in GitHub repo secrets

## Files Changed

| File | Action |
|------|--------|
| `.github/workflows/release.yml` | Updated |

## Verification Checklist

- [ ] `PLAY_STORE_JSON_KEY` secret added to GitHub repository
- [ ] Push tag `v1.3.0` → Actions workflow completes without errors
- [ ] `app/build.gradle.kts` on `main` shows `versionCode = 1003000`, `versionName = "1.3.0"`
- [ ] GitHub Release `v1.3.0` has both `.aab` and `.apk` attached
- [ ] Play Console → Internal testing → draft release visible with uploaded AAB
- [ ] `workflow_dispatch` with `version: v1.3.0` builds successfully without commit-back or Play Store upload
