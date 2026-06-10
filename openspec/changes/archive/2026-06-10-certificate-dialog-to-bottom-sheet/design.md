## Context

`AppDetailScreen` currently renders its content inside a Material 3 `AlertDialog`. The dialog is invoked from `AppNavHost` via a Navigation 3 back stack entry (`AppDetail(packageName)`). Dismissal calls `backStack.removeLastOrNull()` through the `onDismiss` callback. The content (loading spinner, error text, certificate rows, history section) lives in `AppDetailContent`, which is self-contained and already handles its own scrolling. The project already uses `ModalBottomSheet` in `AppAboutScreen.kt`, establishing the pattern.

## Goals / Non-Goals

**Goals:**
- Replace `AlertDialog` with `ModalBottomSheet` in `AppDetailScreen.kt`.
- Support swipe-to-dismiss and scrim-tap-to-dismiss with proper hide animation.
- Preserve all existing content: app name header, "tap to copy" hint, share icon, certificate rows, history expand/collapse.
- Add a close `IconButton` (X) in the header for accessibility.
- Keep the spec for `certificate-inspection` accurate by updating "dialog" and "detail view" language to "bottom sheet" throughout.

**Non-Goals:**
- Changing navigation architecture or back-stack logic.
- Modifying the ViewModel, repository, or any domain layer.
- Redesigning the content layout or adding new fields.
- Handling deep-links or multi-pane layouts.

## Decisions

### Decision 1: Use `ModalBottomSheet` from Material 3

`ModalBottomSheet` is the idiomatic M3 component for this pattern. It ships in `androidx.compose.material3` (already a dependency), supports swipe-to-dismiss via `SheetState`, and renders above a scrim that dismisses on tap. An existing usage in `AppAboutScreen.kt` provides a proven pattern for this codebase.

**Alternative considered**: `BottomSheetScaffold` — rejected because it is intended for persistent (non-modal) sheets embedded in a scaffold, not for overlay detail panels triggered by list taps.

### Decision 2: `skipPartiallyExpanded = true`

The sheet will open fully expanded (no half-expanded stop). Certificate content can be long; a half-expanded state would cut off content mid-way. During loading/error states the sheet will be near-fullscreen with empty space — this is accepted as a simpler, consistent behaviour over dynamic sizing.

### Decision 3: Move header into sheet content; add close `IconButton`

`AlertDialog` provides dedicated `title` and `confirmButton` slots. `ModalBottomSheet` has a single `content` lambda. The app name row, "tap to copy" hint, share icon, and a close `IconButton` (X) will be placed in a `Row` at the top of the content column. The "Close" `TextButton` is removed; the X `IconButton` and swipe-to-dismiss replace it. The X button is required (not optional) for accessibility (TalkBack users, keyboard navigation).

### Decision 4: `onDismissRequest` must go through `sheetState.hide()` first

`ModalBottomSheet.onDismissRequest` fires synchronously when the user taps the scrim. Mapping it directly to `onDismiss` (which calls `backStack.removeLastOrNull()`) removes the composable from the tree before the hide animation completes — the sheet disappears with no animation. The correct pattern is:

```kotlin
val scope = rememberCoroutineScope()
ModalBottomSheet(
    onDismissRequest = { scope.launch { sheetState.hide(); onDismiss() } },
    ...
)
```

This matches the pattern used in `AppAboutScreen.kt`.

**Note**: No changes to `AppNavHost` or `AppDetailViewModel` are needed. The `onDismiss` callback contract is unchanged.

### Decision 5: `contentWindowInsets = WindowInsets(0)` + `navigationBarsPadding()` inside content

`ModalBottomSheet` consumes navigation bar insets by default via its `contentWindowInsets` parameter. Adding `navigationBarsPadding()` inside the content column without disabling the sheet-level insets would double-apply them. The correct approach:

```kotlin
ModalBottomSheet(
    contentWindowInsets = WindowInsets(0),
    ...
) {
    Column(Modifier.navigationBarsPadding()) { ... }
}
```

### Decision 6: Migrate `AppDetailContent` from `Column + verticalScroll` to `LazyColumn`

`ModalBottomSheet` uses a nested scroll connection internally for swipe-to-dismiss. A `Column + verticalScroll` inside the sheet does not reliably hand off scroll-to-dismiss gestures when the user reaches the top of the scroll position (known issue b/229267714 in Compose). `LazyColumn` cooperates with `ModalBottomSheet`'s nested scroll automatically. The certificate rows are a bounded, non-dynamic list so a `LazyColumn` is appropriate.

### Decision 7: `SheetState` owned inside `AppDetailScreen`

The `SheetState` is local UI state with no semantic meaning outside the composable. It is created with `rememberModalBottomSheetState(skipPartiallyExpanded = true)` inside `AppDetailScreen` and not exposed to callers.

### Decision 8: History expansion uses `remember(packageName)`, not `rememberSaveable`

`HistoricalCertificatesSection` currently uses `rememberSaveable(packageName)` for its expanded/collapsed state, which restores expansion when the *same* app's sheet is re-opened. Per the spec, the sheet SHALL always start collapsed. Switching to `remember(packageName)` ensures the history section is collapsed every time the sheet opens, for any app.

## Risks / Trade-offs

- **[Predictive Back]** → `ModalBottomSheet` in Compose M3 supports the system predictive-back gesture on API 34+; verify the dismiss animation looks correct on a device/emulator running API 34+.
- **[Content clipping]** → Mitigated by Decision 5 (`contentWindowInsets = WindowInsets(0)` + `navigationBarsPadding()` on content column).
- **[Nested scroll]** → Mitigated by Decision 6 (`LazyColumn` migration).
- **[Double dismiss]** → The `removeLastOrNull()` back-stack call is a no-op if the entry was already removed; no guard needed, but this is an implicit reliance on that behaviour.
- **[`@OptIn` annotation]** → `ModalBottomSheet` and `rememberModalBottomSheetState` require `@OptIn(ExperimentalMaterial3Api::class)` on every `@Composable` in the call chain — `AppDetailScreen` itself and all five preview functions.
- **[Keyboard insets]** → If a future change adds an editable field inside the sheet, the sheet will need `windowSoftInputMode = ADJUST_RESIZE` handling. Not relevant now.
- **[Spec drift]** → All spec scenarios that reference "dialog" or "detail view" must be updated to "bottom sheet" in the same PR.
