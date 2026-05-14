## Context

`gradle/libs.versions.toml` is the single source of truth for library and plugin versions in CertHunter (per `openspec/project.md`). Today it has:

- 21 `[versions]` keys, 33 `[libraries]` entries, 4 `[plugins]` entries, and **zero `[bundles]`** — every consumer in `app/build.gradle.kts` lists individual aliases by hand.
- Mixed-purpose entries: JUnit 4 (`junit`) coexists with JUnit 5 (`junit5-api/engine`) despite the project standard being JUnit 5.
- Inconsistent key naming: `kotlinResult`, `coreKtx`, `composeBom`, `junitVersion`, `espressoCore`, `kotlinxCoroutines` — some are domain-prefixed, some are not; some use noun-only names, some embed `Version` in the key.
- Library aliases mostly follow `<group>-<artifact>` dashed style but a few drop the group (`junit`, `turbine`, `mockk`) and one embeds an intermediate token (`androidx-tools-desugar-jdk-libs`).
- Compose UI libraries appear in five different `app/build.gradle.kts` sections (Compose, debug, instrumented test) — each section is a natural bundle.
- `app/build.gradle.kts` already organises dependencies with `// ─── Section ───` comment banners. The TOML file has no comparable structure.

Stakeholders: contributors writing Gradle changes (every PR that touches dependencies); reviewers verifying versions; future-me extracting modules.

## Goals / Non-Goals

**Goals:**

- Make `libs.versions.toml` self-explanatory: any contributor can find the right place to add a new library in under 30 seconds.
- Eliminate orphan entries and contradictory standards (JUnit 4 vs JUnit 5).
- Replace hand-rolled alias lists in `app/build.gradle.kts` with `libs.bundles.*` for groups that always move together.
- Codify the rules in `openspec/specs/code-conventions/spec.md` so a future audit is a re-run, not a re-discovery.
- Keep the diff trivially reviewable: **zero version bumps** in this change.

**Non-Goals:**

- Bumping any library version — that is a separate, per-library decision and stays out of this PR.
- Introducing Gradle convention plugins / `build-logic`. That was offered as an alternative direction and explicitly deferred.
- Splitting `:app` into feature/core modules. Bundles enable it; the actual split is future work.
- Migrating tests off JUnit 4 patterns *if any remain after the audit* — only the catalog declaration and unused references are removed here.

## Decisions

### Decision 1 — Bundle granularity

Bundle by *role* (what the library does in this project), not by *vendor*. So `compose` (production UI), `compose-debug` (preview tooling), `compose-android-test` (Compose UI test deps), `unit-test` (JUnit 5 + Turbine + Coroutines test + Mockk), `koin`, `navigation3`, `klogx`.

**Why role-based, not vendor-based?** `app/build.gradle.kts` already groups by role (`// ─── Jetpack Compose ───`, `// ─── Unit tests ───`). Bundles that mirror the sections collapse cleanly into one-liners. A vendor bundle (e.g. one big `androidx` bundle) would force `app/build.gradle.kts` to keep using individual aliases, defeating the point.

**Alternative considered:** Skip bundles, only fix naming. Rejected — the catalog is already verbose, and `app/build.gradle.kts` has six places where 2–6 aliases move together.

### Decision 2 — Naming convention

- **Version keys**: `camelCase`, no `Version` suffix (`coreKtx`, not `coreKtxVersion`). Group prefix only when needed for disambiguation (`composeBom`, `kotlinxCoroutines`).
- **Library aliases**: `kebab-case`, shaped as `<group-without-com-or-org>-<artifact>` (`androidx-core-ktx`, `coil-compose`, `kotlinx-serialization-json`, `androidx-compose-ui-test-junit4`). Test-only libraries with no obvious group keep their short bare name (`turbine`, `mockk`) since the consuming line is already `testImplementation(libs.turbine)`.
- **Plugin aliases**: `kebab-case`, `<ecosystem>-<role>` (`android-application`, `kotlin-compose`, `kotlin-serialization`, `detekt`).
- **Bundle aliases**: `kebab-case`, role name only (`compose`, `compose-debug`, `unit-test`).

**Alternative considered:** Full `<group-id>-<artifact>` everywhere (e.g. `io-mockk-mockk`). Rejected — too verbose, hurts readability at the call site.

### Decision 3 — JUnit 4 entry handling

Plan: remove `libs.junit` from the catalog and `testImplementation(libs.junit)` from `app/build.gradle.kts`. The project standard is JUnit 5; `kotlin("test-junit5")` plus `junit5-api/engine` covers production use. Verification: `./gradlew :app:testDebugUnitTest` must pass after removal.

If removal breaks the build (some androidx-test pulls in JUnit 4 transitively and a test references `org.junit.Test` from the JUnit 4 namespace), the fallback is to keep the runtime dependency but rename the alias to `junit4-legacy` with a comment explaining the constraint. This fallback is recorded so reviewers can verify the decision wasn't forgotten.

### Decision 4 — File ordering and section banners

Order top-to-bottom: `[versions]` → `[libraries]` → `[bundles]` → `[plugins]`. Within `[libraries]` and `[versions]`, group entries under `# ─── Section ───` comments matching the seven sections already in `app/build.gradle.kts` (AndroidX core, Compose, Navigation, DI, Networking/image loading, Serialization, Logging, Utilities, Static analysis, Unit tests, Instrumented tests, Debug only).

**Why mirror `app/build.gradle.kts`?** Onboarding a new contributor: open both files side-by-side, the visual structure matches.

### Decision 5 — Spec home

Add the new requirements to the existing `code-conventions` spec rather than creating a new `build-tooling` capability. The `code-conventions` spec already states it "codifies CertHunter's code-style rules… describes how the codebase itself is structured" — version-catalog organization is a natural fit. A separate capability would be over-structured for what is, today, one file.

**Trigger to revisit:** if convention plugins (`build-logic`) are introduced later, splitting out a `build-tooling` capability becomes worthwhile.

## Risks / Trade-offs

- **[Risk]** Renaming a library alias breaks `app/build.gradle.kts` references silently — Gradle reports unresolved `libs.<x>` at configure time, but a typo in the TOML (e.g. dotted name → camelCase mismatch) might not. **Mitigation**: rename in a single commit; the very next command (`./gradlew help`) surfaces every break.
- **[Risk]** A bundle that's *almost* always used together but occasionally split (e.g. one Compose-only module that needs `ui` but not `material3`) leaks the wrong dependency. **Mitigation**: bundles list only aliases used together in *every* current consumer; ad-hoc combinations stay as individual aliases.
- **[Risk]** Removing `libs.junit` breaks an instrumented test path. **Mitigation**: full `:app:testDebugUnitTest` and `:app:assembleDebug` run before commit (already part of the project's pre-commit checks).
- **[Trade-off]** Bundles obscure exact dependencies at the call site — readers must open the catalog to see what `libs.bundles.compose` resolves to. Acceptable: the catalog is the source of truth anyway, and bundle names are role-descriptive.
- **[Trade-off]** Forbidding `Version` suffix on version keys is a small style preference, not a technical requirement. The convention only matters because it has to be *some* consistent choice.

## Migration Plan

1. Land catalog rewrite and `app/build.gradle.kts` updates in one commit so `git bisect` lands on a working state either way.
2. Run pre-commit checks (`:app:detekt`, `:app:testDebugUnitTest`, `:app:assembleDebug`) before pushing.
3. No runtime migration needed — the change is build-time only. Rollback = `git revert`.

## Open Questions

- Is `libs.junit` (JUnit 4) actually unused at runtime? → resolved during implementation by removing it and running the test suite; if it fails, fall back to renaming per Decision 3.
- Should `androidx-tools-desugar-jdk-libs` be renamed to `android-tools-desugar` to drop the redundant `jdk-libs` suffix? → leave the artifact name as-is to match the published Maven coordinates; this is a cosmetic call we can revisit if it bothers anyone.
