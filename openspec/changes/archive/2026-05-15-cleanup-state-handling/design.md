## Context

The audit that motivated this change (see `proposal.md`) flagged nine distinct issues in `ui/list`, `ui/detail`, `ui/AppNavHost.kt`, and `data/repository/SignerSelector.kt`. Five deserve a design decision because they touch how state flows through the app; the others are mechanical fixes that fit naturally in `tasks.md`.

CertHunter's stack constrains the solution space:

- ViewModels expose `StateFlow<UiModel>` and `SharedFlow<Event>`. There is no `Effect`/`Action`/`Reducer` framework.
- Coroutines run through `DispatcherProvider`; no direct `Dispatchers.*` usage in production code (`code-conventions/spec.md` §"Coroutine dispatcher abstraction").
- Composables consume state via `collectAsState()`.

The decisions below explain the chosen approach. Mechanical fixes (re-throw `CancellationException`, simplify `SignerSelector`, hoist version lookup in `AppNavHost`) need no further design.

## Goals / Non-Goals

**Goals:**

- Eliminate silent cancellation swallowing in `AppListViewModel`.
- Prevent stale app-list requests from overwriting newer state.
- Clear stale refresh error state as soon as a new refresh begins.
- Establish a single source of truth for ViewModel-owned state.
- Move filter/sort recomputation out of the UI-model constructor and only recompute when its inputs change.
- Scope detail-dialog local UI state to the currently opened package.
- Codify the resulting conventions in `code-conventions` so the same drift does not recur.

**Non-Goals:**

- Introducing an MVI framework or refactoring to `Reducer<State, Action>`. The existing `StateFlow + onX` callbacks pattern is adequate; the change tightens it, not replaces it.
- Migrating to `Flow.stateIn(WhileSubscribed(5_000))`. The app is single-screen and the existing `MutableStateFlow(initialValue)` is fine.
- Changing user-visible behaviour. Filter/sort outputs must match the current `AppListUiModel.filteredApps` semantics exactly.
- Redesigning navigation or replacing `AlertDialog` state ownership with a new screen-level architecture.

## Decisions

### Decision 1: Coordinate list loads with newest-request-wins semantics

`AppListViewModel` will treat list loads as a single pipeline with one authoritative in-flight request. Starting a newer load/refresh/retry will supersede any older request so late results cannot overwrite newer state. The implementation can use a stored `Job`, request token, or equivalent sequencing primitive; the key contract is that only the newest request may commit list state.

**Rationale.** Today `loadApps()`, `onRefresh()`, and `onRetry()` all launch independent coroutines. That is fine while requests never overlap, but once they do, stale completions can win by timing rather than intent. Explicit newest-request-wins coordination matches the app-inventory behaviour the user actually expects.

**Alternatives considered.**

- Leave concurrent jobs alone and rely on last-writer-wins timing. Rejected: produces nondeterministic visible state.
- Cancel only refresh jobs but not initial load/retry jobs. Rejected: still leaves inconsistent sequencing rules for the same list pipeline.

### Decision 2: Move filter/sort into a derived `StateFlow` in `AppListViewModel`

The displayed list will be exposed as a second flow (or as a member of a `Displayed`-style sub-model) that is built with `combine(...)` over the four inputs that actually affect it (`allApps`, `searchQuery`, `showSystemApps`, `sortOrder`), and surfaced to the screen as `StateFlow<List<AppItem>>`.

**Rationale.** Today `AppListUiModel.filteredApps` is a property initialiser, so every `_uiState.update { it.copy(isRefreshing = true) }` re-runs the entire filter+sort pipeline even though nothing about the list changed. Moving the pipeline into the ViewModel makes the recomputation triggers explicit and matches the project's "derived UI state belongs in the ViewModel layer" convention being added in this change.

**Alternatives considered.**

- `filteredApps by lazy { ... }` inside the UI model. Rejected: each `copy()` produces a new `AppListUiModel` instance, so the lazy is rebuilt every time anyway.
- `@Stable` on `AppListUiModel` and trusting Compose skipping. Rejected: doesn't help, because the *property initialiser* runs at construction, before Compose ever sees the object.
- `derivedStateOf` in the composable. Rejected: pushes business logic into the UI layer, violates the convention we are codifying.

**Shape of the result.** The screen will be passed both the `AppListUiModel` (loading/error flags, search query, etc.) and a `List<AppItem>` for what to render. Wiring detail belongs in `tasks.md`.

### Decision 3: Replace `AppDetailViewModel.loadedDetails` with derivation from `_uiState`

`AppDetails` is already reconstructible from the four fields the ViewModel writes into `_uiState` on success (`appName`, `isSystemApp`, `certificates`, `historicalCertificates`). The `ShareCertificate` action will read those fields directly from `_uiState.value` and build the share text from them.

**Rationale.** The current `private var loadedDetails: AppDetails?` is written from `dispatcherProvider.io` and read from the main thread without `@Volatile`, an atomic, or any happens-before guarantee independent of `StateFlow`. It also duplicates information already in `_uiState`, which is the violation we are codifying as a convention.

**Alternatives considered.**

- Make `loadedDetails` a `MutableStateFlow<AppDetails?>` and combine with `_uiState`. Rejected: still duplicates state and adds a flow for no benefit.
- Promote `AppDetails` into `AppDetailUiModel` as a single `details: AppDetails?` field. Rejected for now: requires reshaping every existing screen call site and existing tests. The cheaper move is to derive on demand inside `onAction`; we can revisit promoting later if a second consumer appears.

### Decision 4: Make refresh retry clear stale error state immediately

`onRefresh()` will clear `hasRefreshError` at refresh start instead of waiting for a successful result. The existing app list remains visible throughout the refresh.

**Rationale.** Once the user has started a new refresh, the old refresh failure is no longer the current truth. Keeping that banner visible during the retry conflates past failure with present work.

**Alternatives considered.**

- Clear the banner only on success. Rejected: keeps stale failure UI visible during a live retry.
- Add a second "retrying after error" flag and keep the old banner visible. Rejected: more state for no practical UX gain.

### Decision 5: Key detail-dialog local state by package identity

Dialog-local UI state that should reset between different packages, starting with the historical-certificates expansion state, will be keyed by `packageName` so saveable state restoration is scoped to the active detail session.

**Rationale.** `rememberSaveable` is desirable for dialog recreation, but unkeyed saveable state can survive across logically different app-detail sessions. Tying it to package identity keeps the resilience benefit without leaking UI state between apps.

**Alternatives considered.**

- Drop `rememberSaveable` entirely and use `remember`. Rejected: loses state across configuration change / process recreation within the same detail session.
- Lift expansion state into `AppDetailViewModel`. Rejected: unnecessary persistence of purely local presentation state.

### Decision 6: Encode the new conventions in `code-conventions` as SHALL/SHALL NOT rules

Three new requirements are added: cancellation propagation in coroutine catch blocks, no duplicate ViewModel state, and derived UI state in the ViewModel layer. Each one is scoped to a reviewable scenario (matching the existing pattern in `code-conventions/spec.md`).

**Rationale.** The audit found these issues *because* there was no written rule. Encoding them turns "the audit caught this" into "review catches this" for future changes.

## Risks / Trade-offs

- **Risk:** Exposing the list as a second flow changes the `AppListScreen` signature.
  **Mitigation:** Update the screen, preview composables, and the existing unit test in one commit. The behavioural contract (what apps appear in what order) is unchanged; only the path is moved.

- **Risk:** Cancelling or superseding list loads can accidentally drop the visible loading state if the sequencing logic is split across too many helpers.
  **Mitigation:** Keep one authoritative helper that owns both the request lifecycle and the final state commit path; test overlap cases explicitly.

- **Risk:** Tests that exercise `AppListUiModel.filteredApps` directly (currently `AppListUiModelTest`) lose their target.
  **Mitigation:** Move equivalent assertions into `AppListViewModelTest` using Turbine on the new derived flow. The scenarios stay the same; only the SUT changes.

- **Risk:** Removing `loadedDetails` and reading `_uiState.value` inside `onAction` ties share behaviour to the snapshot timing of state updates.
  **Mitigation:** The existing `_uiState.update { ... certificates = ... }` already publishes atomically; reading `_uiState.value` after the update returns the same snapshot. This is equivalent to the current "set var before publishing state" ordering.

- **Risk:** Keying history expansion by package name alone may preserve state unexpectedly if the same package is reopened within the same back-stack session.
  **Mitigation:** That is acceptable for this change because the bug is cross-package leakage. If product wants every open to reset even for the same package, that can be a later requirement.

- **Trade-off:** No `WhileSubscribed` lifecycle for the new derived flow. CertHunter is single-Activity with a single observed list, so the simpler `MutableStateFlow` mirror (or eager `stateIn(viewModelScope, Eagerly, emptyList())`) is enough; revisiting only makes sense if a backgrounded screen becomes a concern.

## Migration Plan

No data migration. Internal-only refactor; rollback is `git revert` of the implementing PR. CI (lint + unit tests) is the gate.

## Open Questions

None. Tasks proceed straight from this design.
