# Release Automation Design — Continuous Distribution to Google Play Store

**Date:** 2026-04-18
**Status:** Approved

## Problem

CertHunter's original release workflow triggered on `push: tags: v*`, which created a fundamental ordering problem: the developer pushed a tag at the pre-version-bump commit, the workflow patched `build.gradle.kts` and committed back to `main`, but the tag was left permanently pointing to the old commit where `versionName` still held the previous version. Tag ref and source were mismatched forever.

## Solution

Remove the tag trigger entirely. The workflow owns the full release sequence:
patch version → build → commit → **create tag at the version-bumped commit** → push → publish.

The tag is always created by CI and always points to the correct commit.

## Goals

- Trigger releases manually via `workflow_dispatch` (version + release type as inputs)
- Build both AAB (Play Store) and APK (sideloading)
- Upload the AAB as a draft to the Play Store internal testing track
- Attach both AAB and APK to the GitHub Release
- Tag always points to the commit where `build.gradle.kts` matches the released version
- Support both regular releases (from `main`) and hotfix releases (from a prepared hotfix branch)

## Non-Goals

- Managing Play Store store listing metadata, screenshots, or release notes via CI
- Automatically promoting from internal to alpha/beta/production (stays manual in Play Console)

## Architecture

### Trigger

`workflow_dispatch` only.

| Input | Type | Required | Description |
|-------|------|----------|-------------|
| `version` | string | yes | Version to release, e.g. `1.3.0` or `v1.3.0` |
| `release_type` | choice | yes | `release` (from `main`) or `hotfix` (from hotfix branch) |

### Branch Rules

| release_type | Required branch | Behaviour |
|---|---|---|
| `release` | `main` | Fails if triggered from any other branch |
| `hotfix` | any branch except `main` | Developer prepares e.g. `hotfix/1.2.1` with the fix commits, then triggers from there |

### Pipeline Steps

```
workflow_dispatch (version=1.3.0, release_type=release, branch=main)
        │
        ▼
1.  Checkout branch (fetch-depth: 0)
2.  Validate: semver format, branch matches release_type rule, tag does not yet exist
3.  Patch app/build.gradle.kts (local only — build uses this)
4.  Set up JDK 21 + Gradle
5.  Decode keystore
6.  Build signed AAB + APK
7.  Upload AAB to Play Store internal track (draft)
8.  Commit version bump on current branch
9.  Create git tag v{version} at that commit + push commit + push tag
10. Create GitHub Release with AAB + APK
```

If any step fails before step 8, the repo is left untouched — no commit, no tag, no published release.

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

Hotfixes always have a lower versionCode than their successor minor release, preserving correct semantic ordering.

### Play Store Upload

Tool: `r0adkll/upload-google-play@v1`
- Package: `com.mohsenoid.certhunter`
- Track: `internal`
- Status: `draft` (requires human publish click in Play Console before testers see it)

### GitHub Release

Uses `softprops/action-gh-release@v2` with explicit `tag_name` (the tag pushed in step 9). Attaches both:
- `app/build/outputs/bundle/release/*.aab`
- `app/build/outputs/apk/release/*.apk`

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

## Notes on `[skip ci]`

The commit message does not include `[skip ci]`. The CI workflow (`ci.yml`) only triggers on `pull_request` and `workflow_dispatch`, so a direct push to `main` never runs CI regardless.

## Files Changed

| File | Action |
|------|--------|
| `.github/workflows/release.yml` | Updated |

## Verification Checklist

- [ ] `PLAY_STORE_JSON_KEY` secret added to GitHub repository
- [ ] Trigger from feature branch with `release_type=release` → fails (not main)
- [ ] Trigger from `main` with invalid version → fails (format error)
- [ ] Trigger from `main` with `release_type=hotfix` → fails (hotfix from main)
- [ ] Trigger from `main` with `version=1.3.0`, `release_type=release` → succeeds:
  - `app/build.gradle.kts` shows `versionCode = 1003000`, `versionName = "1.3.0"`
  - Tag `v1.3.0` points to that exact commit
  - GitHub Release `v1.3.0` has both `.aab` and `.apk`
  - Play Console → Internal testing shows a draft release
- [ ] Retrigger same version → fails (tag already exists)
- [ ] Trigger from `hotfix/1.2.1` branch with `version=1.2.1`, `release_type=hotfix` → succeeds with tag on hotfix branch
