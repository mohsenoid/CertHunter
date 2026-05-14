## 1. Audit current usage

- [x] 1.1 Cross-reference every `[versions]`, `[libraries]`, and `[plugins]` entry in `gradle/libs.versions.toml` against `app/build.gradle.kts` and any other build script that may reference them; list orphans.
- [x] 1.2 Confirm whether `libs.junit` (JUnit 4) is actually required: try removing it (line 129 of `app/build.gradle.kts`) and the catalog entry, then run `./gradlew :app:testDebugUnitTest`. Record the outcome.
- [x] 1.3 Capture the audit findings in a short note inside the change directory (`notes.md` or inline at top of `tasks.md` as TODO comments) so reviewers can verify the orphan list.

## 2. Rewrite the catalog

- [x] 2.1 Reorder `gradle/libs.versions.toml` into top-to-bottom sections `[versions]` → `[libraries]` → `[bundles]` → `[plugins]`.
- [x] 2.2 Within `[versions]` and `[libraries]`, add `# ─── Section ───` comment banners mirroring the sections in `app/build.gradle.kts` (AndroidX core, Compose, Navigation, DI, Image loading, Serialization, Logging, Utilities, Static analysis, Unit tests, Instrumented tests, Debug only).
- [x] 2.3 Rename version keys to `camelCase` with no `Version` suffix per the design's naming convention; update every `version.ref` referencing them.
- [x] 2.4 Normalise library aliases to the `<group-stem>-<artifact>` kebab-case form per the design; do not touch test-only single-token aliases (`turbine`, `mockk`).
- [x] 2.5 Remove every orphan entry identified in step 1.1.
- [x] 2.6 Apply the JUnit 4 outcome from step 1.2: either remove `libs.junit` and its consumer, or rename to `junit4-legacy` with an inline comment explaining the constraint.

## 3. Add bundles

- [x] 3.1 Add `[bundles]` section with `compose` (ui, ui-graphics, ui-tooling-preview, material3, material-icons-core, material-icons-extended).
- [x] 3.2 Add `compose-debug` (ui-tooling, ui-test-manifest).
- [x] 3.3 Add `compose-android-test` (ui-test-junit4 only). `ui-test-manifest` is `debugImplementation` in the current build and belongs to the `compose-debug` bundle.
- [x] 3.4 Add `navigation3` (navigation3-runtime, navigation3-ui).
- [x] 3.5 Add `koin` (koin-android, koin-androidx-compose).
- [x] 3.6 Add `klogx` (klogx-core, klogx-android-logcat). Leave the BOM (`klogx-bom`) as an individual alias since it's consumed via `platform(...)`.
- [x] 3.7 Add `unit-test` (junit5-api, kotlinx-coroutines-test, turbine, mockk). Keep `junit5-engine` as an individual alias because it is consumed via `testRuntimeOnly`, not `testImplementation`.

## 4. Update consumers

- [x] 4.1 Replace individual Compose alias lines in `app/build.gradle.kts` (lines 95–101) with `implementation(libs.bundles.compose)` (keep the BOM line above).
- [x] 4.2 Replace navigation lines (104–105) with `implementation(libs.bundles.navigation3)`.
- [x] 4.3 Replace Koin lines (108–109) with `implementation(libs.bundles.koin)`.
- [x] 4.4 Replace KLogX core+logcat lines (119–120) with `implementation(libs.bundles.klogx)` (keep the BOM `platform(...)` line above).
- [x] 4.5 Replace unit-test lines (130, 133–135) with `testImplementation(libs.bundles.unit.test)`. Keep `kotlin("test-junit5")` and `testRuntimeOnly(libs.junit5.engine)` as-is.
- [x] 4.6 Replace instrumented-test Compose line (141) with `androidTestImplementation(libs.bundles.compose.android.test)`.
- [x] 4.7 Replace debug Compose lines (144–145) with `debugImplementation(libs.bundles.compose.debug)`.
- [x] 4.8 Update any other module-level build scripts (if discovered during audit) to use the new aliases and bundles. No other module-level scripts exist (single :app module). Root `build.gradle.kts` only references plugin aliases which are unchanged. Also renamed `androidx-junit` → `androidx-test-ext-junit` and `androidx-espresso-core` → `androidx-test-espresso-core` for accuracy (instrumented-test consumers updated accordingly).

## 5. Update the spec

- [x] 5.1 Edit `openspec/specs/code-conventions/spec.md` to add the four new requirements introduced by this change (single source of truth, naming convention, bundles, no orphans, mixed-JUnit ban) — see `specs/code-conventions/spec.md` in this change for exact text.
- [x] 5.2 Verify the spec edit aligns with the existing file's tone and section ordering; preserve all pre-existing requirements unchanged.

## 6. Verify

- [x] 6.1 Run `./gradlew help` to surface any unresolved `libs.<alias>` references from the renames. BUILD SUCCESSFUL.
- [x] 6.2 Run `./gradlew :app:detekt` and fix or baseline any new violations. BUILD SUCCESSFUL (no new violations).
- [x] 6.3 Run `./gradlew :app:testDebugUnitTest` and confirm all tests pass. BUILD SUCCESSFUL.
- [x] 6.4 Run `./gradlew :app:assembleDebug` to confirm the debug variant still builds. BUILD SUCCESSFUL.
- [x] 6.5 Diff `git diff -- gradle/libs.versions.toml app/build.gradle.kts` and confirm zero version-number changes. Confirmed: every retained version string is identical pre/post; the only `-` line with a version number is the removed `junit = "4.13.2"` orphan, and `junitVersion = "1.3.0"` was renamed to `androidxTestExt = "1.3.0"` (same value).

## 7. Archive prep

- [x] 7.1 Run `openspec status --change cleanup-version-catalog` to confirm all artifacts are done.
- [x] 7.2 Update `openspec/project.md` only if a wording reference to the catalog must change (the existing "authoritative source for library versions" line should stay).
- [x] 7.3 Open the PR with the proposal summary and link this change directory in the PR description. (PR #23 opened; archival happens after merge.)
