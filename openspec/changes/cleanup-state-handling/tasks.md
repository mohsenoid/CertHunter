## 1. AppListViewModel: cancellation + duplication

- [x] 1.1 In `AppListViewModel.loadApps()` and `onRefresh()`, re-throw `kotlinx.coroutines.CancellationException` before the existing logging / state-update path.
- [x] 1.2 Coordinate `loadApps()`, `onRefresh()`, and `onRetry()` so only the newest request may commit app-list state; older overlapping work is cancelled or ignored.
- [x] 1.3 Extract a single private suspend/helper pipeline that runs `repository.getInstalledApps()`, applies success/error state updates via lambdas, and re-throws `CancellationException`. Call it from both `loadApps()` and `onRefresh()`.
- [x] 1.3 Keep the `@Suppress("TooGenericExceptionCaught")` annotation only on the helper, with a comment that references the new `code-conventions` requirement and explains that the cancellation rethrow guarantees structured cancellation.
- [x] 1.4 Clear `hasRefreshError` as soon as a new refresh starts, while preserving the existing app list during the retry.
- [x] 1.5 Add a unit test in `AppListViewModelTest` that cancels the load coroutine and asserts (a) `hasLoadError` stays `false`, (b) the test does not hang, (c) cancellation completes normally.
- [x] 1.6 Add an overlap test in `AppListViewModelTest` showing that a slower older request cannot overwrite the result of a newer request.
- [x] 1.7 Add a refresh-retry test in `AppListViewModelTest` showing that the refresh error banner clears immediately when a new refresh begins.

## 2. AppListViewModel + AppListUiModel: derived list

- [x] 2.1 Remove the `filteredApps: List<AppItem>` property initialiser from `AppListUiModel`.
- [x] 2.2 In `AppListViewModel`, expose a derived `StateFlow<List<AppItem>>` (e.g. `displayedApps`) using `combine(allApps, searchQuery, showSystemApps, sortOrder)` (source the four inputs from `_uiState`, e.g. via `map { it.allApps } / distinctUntilChanged()` or by splitting them into private MutableStateFlows — pick whichever is cleaner).
- [x] 2.3 Materialise the derived flow with `stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())` so the screen sees the initial value synchronously.
- [x] 2.4 Update `AppListScreen` to accept the displayed list as a separate parameter (or as a sub-object on the UI model) and stop reading `uiState.filteredApps`.
- [x] 2.5 Update `AppNavHost` to pass the new derived list into `AppListScreen` alongside the UI model.
- [x] 2.6 Update or remove `AppListUiModelTest` so the filter/sort assertions live on the new derived flow; cover the same scenarios in `AppListViewModelTest` using Turbine. (Removed `AppListUiModelTest`; 10 equivalent scenarios ported to `AppListViewModelTest` using `displayedApps.value` plus one Turbine test verifying `isRefreshing` toggles do not re-emit.)
- [x] 2.7 Update `AppListScreen` preview composables so they continue to render with the new parameter shape.

## 3. AppDetailViewModel: drop the mutable mirror

- [x] 3.1 Remove `private var loadedDetails: AppDetails?` and the `loadedDetails = it` assignment in the `onSuccess` block.
- [x] 3.2 Rework `onAction(ShareCertificate)` to derive an `AppDetails` from `_uiState.value` (the four fields it needs are already there) and pass it through `toShareText(...)`.
- [x] 3.3 Guard the share path with the existing `canShare` semantics: do not emit the share event when `isLoading`, `error != null`, or `certificates.isEmpty()`.
- [x] 3.4 Update `AppDetailViewModelTest` to verify share still emits after a successful load and now also covers the loading/error/empty cases where `ShareCertificate` should be a no-op.

## 4. AppDetailScreen: package-scoped local state

- [x] 4.1 Key the historical-certificates expansion `rememberSaveable` state by `uiState.packageName` (or equivalent session identity) so local expansion does not leak across different app-detail dialogs.
- [ ] 4.2 Add UI-state coverage or a focused regression test to capture the cross-package history-expansion case. (Deferred — no Compose UI test infrastructure exists in `androidTest/` yet; setting up `createComposeRule()` plus the activity host is out of scope for this change. The behavioural change is encoded in the `rememberSaveable(packageName)` keying and the spec scenario; a follow-up should add an instrumented test once Compose UI testing infra lands.)

## 5. AppNavHost: hoist the About-screen version lookup

- [x] 4.1 Move the `packageManager.getPackageInfo(...).versionName` call out of the composable body and into a `remember { ... }` block keyed by `context.packageName`, so it runs once per About screen entry rather than once per recomposition.
- [x] 4.2 Keep the existing `NameNotFoundException` fallback to `unknownVersion`.

## 6. SignerSelector: simplify and tighten tests

- [x] 5.1 Replace `if (historyBytes.size > 1) historyBytes.dropLast(1) else emptyList()` with `historyBytes.dropLast(1)` in the non-multi-signed branch; update the comment to match.
- [x] 5.2 Add unit tests in `SignerSelectorTest` for: empty history → empty historical list; size-1 history → empty historical list; size-2 history → first entry returned as historical. (Added empty-history test; size-1 and size-2 cases were already covered by existing tests.)

## 7. code-conventions delta + project housekeeping

- [x] 6.1 Run `./gradlew :app:detekt` and address any new findings (or baseline with justification — but prefer fixes).
- [x] 6.2 Run `./gradlew :app:testDebugUnitTest` and ensure the suite passes locally.
- [x] 6.3 Run `openspec validate cleanup-state-handling --strict` once tasks are checked off; ensure it stays green.
- [ ] 6.4 Run `openspec archive cleanup-state-handling` after the implementation PR merges so the `code-conventions` delta folds into the living spec.
