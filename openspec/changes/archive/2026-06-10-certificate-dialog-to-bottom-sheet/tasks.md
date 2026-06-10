## 1. Replace AlertDialog with ModalBottomSheet

- [x] 1.1 In `AppDetailScreen.kt`, replace the `AlertDialog` import with `ModalBottomSheet`, `rememberModalBottomSheetState`, `ExperimentalMaterial3Api`, `rememberCoroutineScope`, and `WindowInsets` imports; remove the now-unused `AlertDialog` and `TextButton` imports; add `@OptIn(ExperimentalMaterial3Api::class)` on the `AppDetailScreen` composable function and on every `@ComponentPreviews` preview function in the file
- [x] 1.2 Add `val scope = rememberCoroutineScope()` and `val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)` inside `AppDetailScreen`
- [x] 1.3 Replace the entire `AlertDialog { ... }` block (including `confirmButton`, `title`, and `text` slots) with `ModalBottomSheet(onDismissRequest = { scope.launch { sheetState.hide(); onDismiss() } }, sheetState = sheetState, contentWindowInsets = WindowInsets(0))`
- [x] 1.4 Inside the sheet's content lambda, add a `Column` with `Modifier.navigationBarsPadding()` and `padding(horizontal = 16.dp)` wrapping all content
- [x] 1.5 At the top of that column, add a header `Row` containing: the app name `Text` (weight 1f), the share `IconButton` (visible when `canShare`), and a close `IconButton` (always visible, calls `{ scope.launch { sheetState.hide(); onDismiss() } }`)
- [x] 1.6 Below the header row, add the "tap to copy" hint `Text` (visible when not loading), then `AppDetailContent`
- [x] 1.7 Add `Modifier.padding(top = 8.dp)` to the header `Row` so the drag handle does not crowd the app name

## 2. Fix Content Layout Inside the Sheet

- [x] 2.1 In `AppDetailContent`, convert the outer `Column(Modifier.verticalScroll(...))` to a `LazyColumn` so certificate rows cooperate with the sheet's nested scroll for swipe-to-dismiss
- [x] 2.2 In `HistoricalCertificatesSection`, change `rememberSaveable(packageName)` to `remember(packageName)` so the history section always starts collapsed when the sheet opens
- [x] 2.3 Update all five `@ComponentPreviews` in `AppDetailScreen.kt` to call `AppDetailScreen(...)` directly inside a `CertHunterTheme { Surface { ... } }` wrapper (do NOT use `BottomSheetScaffold` — it is the wrong component type)

## 3. Update the Spec

- [x] 3.1 In `openspec/specs/certificate-inspection/spec.md`, replace all occurrences of "detail dialog" and "detail view" with "bottom sheet" (requirement headings, scenario bullets, Purpose section)
- [x] 3.2 Rename the requirement heading `"Detail-dialog local UI state is package-scoped"` → `"Detail-sheet local UI state is package-scoped"` and update its body text accordingly
- [x] 3.3 Under "Detail-sheet local UI state is package-scoped", add a new scenario: re-opening the same app's bottom sheet always starts the history section in the default collapsed state (because `remember` is used, not `rememberSaveable`)

## 4. Verify

- [x] 4.1 Run `./gradlew :app:detekt` and fix any new violations
- [x] 4.2 Run `./gradlew :app:testDebugUnitTest` and confirm all tests pass
- [ ] 4.3 Build and install the debug APK; tap an app in the list and confirm the bottom sheet opens from the bottom, all certificate fields are visible, scrolling works without triggering unwanted sheet dismissal, and swipe-to-dismiss and scrim-tap both dismiss smoothly with animation
- [ ] 4.4 Enable TalkBack; open the certificate bottom sheet and verify (a) the close `IconButton` is focusable and announces correctly, (b) the sheet is dismissible via the system accessibility dismiss action
- [ ] 4.5 On an API 34+ device or emulator, verify that the predictive-back swipe on the bottom sheet plays the correct peek-and-dismiss animation
