## Why

A close audit of the ViewModel/state layer surfaced a small cluster of correctness and clarity issues that have accumulated as features landed:

1. `AppListViewModel` wraps its repository calls in `catch (e: Exception)` and silently swallows `kotlinx.coroutines.CancellationException`, breaking cooperative cancellation — a cancelled load is reported back to the UI as a load error.
2. `loadApps()` / `onRefresh()` / `onRetry()` launch independent coroutines without cancellation or sequencing; an older slower request can finish after a newer one and overwrite fresher list state.
3. The `loadApps()` / `onRefresh()` pair duplicates the entire try/catch/state-update pipeline; future tweaks have to be made twice.
4. `onRefresh()` leaves `hasRefreshError` set until success, so the stale error banner remains visible even while a retry refresh is already in progress.
5. `AppDetailViewModel` keeps a mutable `loadedDetails: AppDetails?` field that is written from `dispatcherProvider.io` and read from the UI thread without any synchronisation, even though the same data already lives in `_uiState`.
6. `AppListUiModel.filteredApps` is a property initialiser, so the full filter+sort pipeline re-runs every time *any* field of the UI model changes (e.g. toggling `isRefreshing`) — not just when the inputs that actually drive the list change.
7. `AppDetailScreen` stores the historical-certificates expansion flag in `rememberSaveable` without a key tied to the current package, so expansion state can leak across different app-detail dialogs.
8. `AppNavHost` calls `PackageManager.getPackageInfo` directly inside a composable on every recomposition while the About screen is on top; it should be hoisted off-recomposition.
9. `SignerSelector.select` carries an `if (historyBytes.size > 1)` guard that is redundant with the simpler invariant "drop the last entry of `signingCertificateHistory`".

None of these is user-visible today, but each is a latent foot-gun and they share a root cause (drift in conventions for state derivation and structured concurrency). This change fixes them as one pass and codifies the conventions in `code-conventions` so the same drift does not recur.

## What Changes

- Make `AppListViewModel.loadApps()` and `onRefresh()` re-throw `CancellationException` instead of swallowing it, and extract the common try/catch/state-update pipeline into a single private helper.
- Coordinate list-loading jobs so only the newest request can update visible state, and an in-flight retry supersedes older work instead of racing it.
- Clear stale refresh error state when a new refresh begins, so the retry UI reflects current work rather than the previous failure.
- Replace `AppDetailViewModel.loadedDetails` with a derivation from `_uiState`, eliminating the unsynchronised mutable field.
- Move list filtering and sorting out of `AppListUiModel`'s constructor: derive the displayed list in `AppListViewModel` as a `StateFlow<List<AppItem>>` that recomputes only when `allApps`, `searchQuery`, `showSystemApps`, or `sortOrder` change.
- Key the historical-certificates expansion state in `AppDetailScreen` by package identity so opening a different app always starts from a clean detail-dialog UI state.
- Hoist the About-screen version lookup in `AppNavHost` out of the composable body so it runs once, not on every recomposition.
- Simplify `SignerSelector.select` to `historyBytes.dropLast(1)` for the non-multi-signed branch, removing the redundant size guard.
- Add three new code-conventions requirements:
  - Coroutine `catch` blocks at ViewModel/repository boundaries SHALL re-throw `CancellationException`.
  - ViewModels SHALL NOT hold mutable in-memory state that duplicates fields already exposed in their `StateFlow`.
  - Derived UI state (filter / sort / search pipelines) SHALL be computed in the ViewModel layer, not in the UI-model constructor.
- Add two behavioural capability deltas:
  - `app-inventory`: overlapping list loads SHALL surface only the freshest result, and starting a new refresh SHALL clear the previous refresh error while the retry is in progress.
  - `certificate-inspection`: local dialog UI state such as certificate-history expansion SHALL NOT leak between different package detail sessions.

This is mostly implementation tightening plus convention codification, with two small behavioural clarifications around stale async results and dialog-local state isolation.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `code-conventions`: adds three new requirements covering cancellation propagation, single-source-of-truth ViewModel state, and where derived UI state belongs.
- `app-inventory`: clarifies async freshness and refresh-error clearing semantics.
- `certificate-inspection`: clarifies that detail-dialog local UI state is package-scoped.

## Impact

- **Code**:
  - `app/src/main/java/com/mohsenoid/certhunter/ui/list/AppListViewModel.kt`
  - `app/src/main/java/com/mohsenoid/certhunter/ui/list/AppListUiModel.kt`
  - `app/src/main/java/com/mohsenoid/certhunter/ui/list/AppListScreen.kt` (consume the new derived list)
  - `app/src/main/java/com/mohsenoid/certhunter/ui/detail/AppDetailViewModel.kt`
  - `app/src/main/java/com/mohsenoid/certhunter/ui/detail/AppDetailScreen.kt`
  - `app/src/main/java/com/mohsenoid/certhunter/ui/AppNavHost.kt`
  - `app/src/main/java/com/mohsenoid/certhunter/data/repository/SignerSelector.kt`
- **Tests**:
  - `app/src/test/java/com/mohsenoid/certhunter/ui/list/AppListViewModelTest.kt` (add cancellation propagation, overlap, refresh-banner, and derived-list coverage)
  - `app/src/test/java/com/mohsenoid/certhunter/ui/list/AppListUiModelTest.kt` (filter logic moves out of model; revisit or remove)
  - `app/src/test/java/com/mohsenoid/certhunter/ui/detail/AppDetailViewModelTest.kt` (verify share still works after removing `loadedDetails`)
  - `app/src/test/java/com/mohsenoid/certhunter/ui/detail/AppDetailScreen...` or equivalent UI-state coverage for package-scoped history expansion
  - `app/src/test/java/com/mohsenoid/certhunter/data/repository/SignerSelectorTest.kt` (cover empty history and size-1 history cases)
- **Specs**:
  - delta to `openspec/specs/code-conventions/spec.md`
  - delta to `openspec/specs/app-inventory/spec.md`
  - delta to `openspec/specs/certificate-inspection/spec.md`
- **APIs / dependencies**: none.
- **Runtime behaviour**: user-visible behaviour is unchanged except that stale list results no longer win races, stale refresh errors clear promptly on retry, and history-expansion state resets correctly between different app details.
