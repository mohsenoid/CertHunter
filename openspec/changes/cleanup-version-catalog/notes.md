# Audit notes (task 1.x)

## Inputs scanned

- `gradle/libs.versions.toml`
- `build.gradle.kts` (root)
- `app/build.gradle.kts`
- `settings.gradle.kts`
- All `*.kt` test sources under `app/src/test/` and `app/src/androidTest/`

(Project has a single `:app` module today.)

## Orphan inventory

| Catalog entry | Status | Evidence |
|---|---|---|
| `[libraries] junit` (JUnit 4) | **Orphan** — remove | Only consumer is `app/build.gradle.kts:129 testImplementation(libs.junit)`. No unit test imports `org.junit.*` (JUnit 4). All unit tests import `org.junit.jupiter.api.*` (JUnit 5). The lone JUnit 4 import is `app/src/androidTest/.../ExampleInstrumentedTest.kt`, which gets JUnit 4 transitively from `androidx.test.ext.junit`. |
| `[versions] junit` | **Orphan** — remove (after library removed) | Only referenced by the now-orphan `[libraries] junit` entry. |
| Everything else | In use | Cross-checked against `libs.*` references in `build.gradle.kts` and `app/build.gradle.kts`. |

## Naming-convention violations to fix

| Existing key | Issue | Renamed to |
|---|---|---|
| `junitVersion` | `Version` suffix forbidden by spec | `androidxTestExt` (reflects the actual artifact `androidx.test.ext:junit`) |

All other `[versions]` keys already comply with the camelCase / no-`Version`-suffix rule.

## Library alias normalisation

All library aliases already follow `<group-stem>-<artifact>` style. The single-token test aliases `turbine` and `mockk` keep their bare names per the explicit spec exemption.

## JUnit 4 outcome (task 1.2)

Remove `libs.junit` (catalog entry) and `testImplementation(libs.junit)` (consumer). No unit test depends on JUnit 4. The instrumented test's transitive JUnit 4 dependency is unaffected because it flows through `androidx.test.ext.junit`.

## Bundles to introduce

Per `app/build.gradle.kts` section banners:

| Bundle | Members | Consumer line(s) replaced |
|---|---|---|
| `compose` | androidx-compose-ui, ui-graphics, ui-tooling-preview, material3, material-icons-core, material-icons-extended | 96–101 |
| `compose-debug` | androidx-compose-ui-tooling, ui-test-manifest | 144–145 |
| `compose-android-test` | androidx-compose-ui-test-junit4 | 141 |
| `navigation3` | navigation3-runtime, navigation3-ui | 104–105 |
| `koin` | koin-android, koin-androidx-compose | 108–109 |
| `klogx` | klogx-core, klogx-android-logcat | 119–120 |
| `unit-test` | junit5-api, kotlinx-coroutines-test, turbine, mockk | 130, 133–135 |

BOMs (`androidx-compose-bom`, `klogx-bom`) and `junit5-engine` (runtimeOnly) stay as individual aliases.
