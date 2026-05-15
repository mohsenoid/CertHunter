## ADDED Requirements

### Requirement: Version catalog is the single source of truth

The codebase SHALL declare every third-party library and Gradle plugin in `gradle/libs.versions.toml`. Module-level `build.gradle.kts` files SHALL reference dependencies through `libs.<alias>` or `libs.bundles.<alias>`. Hardcoded coordinate strings (e.g. `implementation("group:artifact:1.2.3")`) SHALL NOT appear in module build scripts.

The following non-`libs.*` forms are explicitly permitted because the version is still resolved through the catalog or the Kotlin Gradle plugin:

- `platform(libs.<alias>)` for BOMs.
- `kotlin("<artifact>")` and `kotlin("<artifact>", version = ...)` (Kotlin Gradle plugin helper), since the Kotlin version is managed by the Kotlin plugin which is itself declared in the catalog.

Libraries whose version is managed by a BOM (e.g. Compose libraries under `platform(libs.androidx.compose.bom)`) MAY omit `version.ref` in their `[libraries]` entry. In that case the catalog entry SHALL declare `group` and `name` only.

Gradle's Kotlin DSL converts kebab-case (`-`) and dotted (`.`) segments in aliases to nested accessors. The catalog alias `androidx-core-ktx` is consumed as `libs.androidx.core.ktx`, and the bundle alias `unit-test` is consumed as `libs.bundles.unit.test`. Write the kebab-case form in `libs.versions.toml` and the dotted form in `build.gradle.kts`.

#### Scenario: Adding a new versioned library

- **WHEN** a contributor adds a new third-party library that is not managed by a BOM
- **THEN** the version is declared under `[versions]` in `libs.versions.toml`
- **AND** the library alias is declared under `[libraries]` with `version.ref` pointing at that version
- **AND** the consuming module's `build.gradle.kts` references the library via `libs.<alias>` or via a bundle that contains it

#### Scenario: Adding a new BOM-managed library

- **WHEN** a contributor adds a Compose (or other BOM-managed) library
- **THEN** the library alias is declared under `[libraries]` with only `group` and `name` (no `version.ref`)
- **AND** the consuming module applies the BOM via `implementation(platform(libs.<bom-alias>))` once
- **AND** every member library is consumed via `libs.<alias>` or `libs.bundles.<alias>`

#### Scenario: Reviewing a build script change

- **WHEN** a pull request modifies a module-level `build.gradle.kts`
- **THEN** no hardcoded `"group:artifact:version"` coordinate appears in the diff
- **AND** every new dependency line uses `libs.<alias>`, `libs.bundles.<alias>`, `platform(libs.<alias>)`, or `kotlin("...")`

### Requirement: Version catalog naming convention

The catalog SHALL use a consistent naming style:

- `[versions]` keys SHALL be `camelCase`. The key SHALL NOT include a `Version` suffix (use `coreKtx`, not `coreKtxVersion`). A domain prefix (`compose`, `kotlinx`, `androidx`) SHALL be included only when needed to disambiguate.
- `[libraries]` aliases SHALL be `kebab-case`, shaped `<group-stem>-<artifact>` where the group stem omits the leading `com.` / `org.` (`androidx-core-ktx`, `coil-compose`, `kotlinx-serialization-json`). Single-token test-only libraries (`turbine`, `mockk`) MAY keep their bare artifact name.
- `[bundles]` aliases SHALL be `kebab-case` role names (`compose`, `compose-debug`, `unit-test`), not vendor names.
- `[plugins]` aliases SHALL be `kebab-case` `<ecosystem>-<role>` (`android-application`, `kotlin-compose`).

#### Scenario: Adding a new version key

- **WHEN** a new version entry is added under `[versions]`
- **THEN** the key is camelCase
- **AND** the key does not end with `Version`

#### Scenario: Adding a new library alias

- **WHEN** a new entry is added under `[libraries]`
- **THEN** the alias is kebab-case
- **AND** the alias starts with the group stem (without `com.`/`org.` prefix) followed by the artifact name, EXCEPT for single-token test-only libraries (`turbine`, `mockk` and similar) which MAY keep their bare artifact name

### Requirement: Bundles for co-used libraries

The catalog SHALL declare a `[bundles]` entry whenever two or more library aliases are consumed together in every current consumer module. Consumer modules SHALL prefer `libs.bundles.<name>` over listing the same aliases individually.

A single-item bundle MAY be declared when it represents a stable role slot expected to grow (e.g. `compose-android-test`), so that consumer modules refer to a role rather than to a specific dependency.

Bundle aliases follow the same kebab-case-to-dotted-accessor mapping as library aliases: `unit-test` in `libs.versions.toml` is consumed as `libs.bundles.unit.test` in `build.gradle.kts`.

The following role-based bundles SHALL exist as long as their member libraries remain in use:

- `compose` — production Compose UI dependencies (`androidx-compose-ui`, `androidx-compose-ui-graphics`, `androidx-compose-ui-tooling-preview`, `androidx-compose-material3`, `androidx-compose-material-icons-core`, `androidx-compose-material-icons-extended`).
- `compose-debug` — `debugImplementation`-only Compose tooling (`androidx-compose-ui-tooling`, `androidx-compose-ui-test-manifest`).
- `compose-android-test` — Compose UI test dependencies for instrumented tests (`androidx-compose-ui-test-junit4`).
- `navigation3` — Navigation 3 (`navigation3-runtime`, `navigation3-ui`).
- `koin` — Koin Android + Compose (`koin-android`, `koin-androidx-compose`).
- `klogx` — KLogX runtime libraries (`klogx-core`, `klogx-android-logcat`). The KLogX BOM is applied via `platform(libs.klogx.bom)` and is not part of the bundle.
- `unit-test` — JUnit 5 API, kotlinx coroutines test, Turbine, MockK (`junit5-api`, `kotlinx-coroutines-test`, `turbine`, `mockk`).

#### Scenario: Multiple modules use the same group together

- **WHEN** more than one consumer module imports the same set of two or more library aliases
- **THEN** those aliases are grouped into a `[bundles]` entry
- **AND** each consumer module references the bundle instead of the individual aliases

#### Scenario: Consuming Compose UI in a module

- **WHEN** a module's `build.gradle.kts` adds production Compose UI dependencies
- **THEN** the module declares `implementation(libs.bundles.compose)` (in addition to the Compose BOM)
- **AND** the module does not list `libs.androidx.compose.ui`, `libs.androidx.compose.material3`, etc. individually

### Requirement: No orphan entries in the catalog

Every alias declared under `[versions]`, `[libraries]`, `[bundles]`, and `[plugins]` SHALL be referenced by at least one consumer (a module build script or another catalog entry). Orphan entries SHALL be removed in the same change that removes their last consumer.

#### Scenario: Removing the last consumer of a library

- **WHEN** a pull request removes the last `libs.<alias>` reference from any module
- **THEN** the same pull request also removes the matching `[libraries]` entry from `libs.versions.toml`
- **AND** removes the corresponding `[versions]` key if no other library references it

#### Scenario: Auditing the catalog

- **WHEN** the catalog is audited (during this change or any future cleanup)
- **THEN** every `[versions]` key has at least one `[libraries]` or `[plugins]` entry referencing it
- **AND** every `[libraries]` alias is referenced by either a module build script or a `[bundles]` entry
- **AND** every `[bundles]` alias is referenced by at least one module build script

### Requirement: Mixed JUnit versions are not allowed

The codebase SHALL declare JUnit 5 (Jupiter) as the unit-test framework. JUnit 4 SHALL NOT be declared as a direct dependency in `[libraries]` unless a documented technical constraint (for example, an instrumented test runner that only supports JUnit 4) requires it. When such a constraint exists, the alias SHALL be named to signal its scope (e.g. `junit4-legacy`) and a comment in the catalog SHALL state the reason.

#### Scenario: Adding a JUnit 4 dependency

- **WHEN** a contributor proposes adding a JUnit 4 declaration to the catalog
- **THEN** the pull request description documents the specific technical reason it cannot use JUnit 5
- **AND** the alias name includes a scope marker such as `legacy`
- **AND** a comment beside the entry restates the reason
