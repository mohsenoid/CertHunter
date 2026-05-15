## Why

`gradle/libs.versions.toml` has grown organically and now mixes JUnit 4 and JUnit 5 entries, lacks bundles for the obvious groupings (Compose UI, Compose debug, Compose android-tests, tests, navigation, Koin, KLogX), uses inconsistent key naming, and has at least one likely-unused declaration (`junit` / JUnit 4) that contradicts the project's JUnit 5 standard. The catalog is the single source of truth referenced by `openspec/project.md`, so drift here quietly degrades contributor onboarding and PR review quality. Cleaning it up now is cheap, low risk, and unblocks future module splits.

## What Changes

- Audit every `[versions]`, `[libraries]`, and `[plugins]` entry against actual usage in `app/build.gradle.kts` and remove orphans.
- Remove the JUnit 4 dependency (`libs.junit`) if confirmed unused; if it must stay for a transitive/runner reason, rename and document why.
- Introduce `[bundles]` for the natural groupings already wired up by hand in `app/build.gradle.kts`:
  - `compose` (ui, ui-graphics, ui-tooling-preview, material3, material-icons-core, material-icons-extended)
  - `compose-debug` (ui-tooling, ui-test-manifest — both are `debugImplementation` today)
  - `compose-android-test` (ui-test-junit4 — `ui-test-manifest` is `debugImplementation` and belongs in the `compose-debug` bundle, not here)
  - `navigation3` (runtime, ui)
  - `koin` (koin-android, koin-androidx-compose)
  - `klogx` (klogx-core, klogx-android-logcat — kept alongside the existing BOM platform call)
  - `unit-test` (junit5-api, kotlinx-coroutines-test, turbine, mockk)
- Normalise version keys to a single convention (camelCase, no `Version` suffix, domain prefix only when needed to disambiguate) and library aliases to `<group-stem>-<artifact>` dashed style.
- Reorder the file with clear comment banners matching the `// ─── Section ───` banners already used in `app/build.gradle.kts`.
- Update `app/build.gradle.kts` to consume the new bundles instead of listing individual aliases.
- Document the catalog conventions in `openspec/specs/code-conventions/spec.md` so future PRs are reviewed against an explicit rule, not tribal knowledge.
- **No version bumps.** This change is conventions + structure only; library versions stay byte-identical so the diff is easy to review and revert.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `code-conventions`: Add a new requirement set for the Gradle version catalog covering (a) one-source-of-truth rule, (b) naming conventions for version keys and library aliases, (c) bundle organization, and (d) no-orphan-entries rule. Existing code-level conventions are unchanged.

## Impact

- **Files changed**: `gradle/libs.versions.toml`, `app/build.gradle.kts`, `openspec/specs/code-conventions/spec.md` (via delta).
- **APIs**: none — purely build-side.
- **Dependencies**: net zero (unused declarations removed, no versions bumped).
- **Risk**: Low. Compile-time errors surface any mis-renamed alias immediately. CI (`:app:detekt`, `:app:testDebugUnitTest`, `:app:assembleDebug`) gates the change.
- **Follow-ups unlocked**: future module splits (e.g., extracting `:core:*` or feature modules) can consume the same bundles from day one without re-litigating naming.
