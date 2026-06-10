## Why

The certificate detail view currently opens in a dialog, which feels modal and abrupt on mobile. A bottom sheet provides a more natural, gesture-friendly interaction pattern that fits Android's Material 3 design language — users can swipe to dismiss and see the app list context behind the detail view.

## What Changes

- The certificate detail screen that currently renders as an `AlertDialog` composable will be replaced with a `ModalBottomSheet` composable.
- The bottom sheet will open from the bottom of the screen and support swipe-to-dismiss and scrim-tap to dismiss.
- All existing content (certificate fields, copy-to-clipboard, share action, historical certificates section) is preserved inside the sheet; the header (app name, share icon) moves from the dialog's `title` slot into the sheet's content column.
- A close `IconButton` (X) is added alongside the share icon in the header for accessibility.
- The history expansion state will use `remember` (not `rememberSaveable`) so it always starts collapsed when the sheet opens.
- The spec language for `certificate-inspection` will be updated: "dialog" and "detail view" replaced with "bottom sheet" throughout.

## Capabilities

### New Capabilities

*(none — this is a presentation-layer reshaping of an existing capability)*

### Modified Capabilities

- `certificate-inspection`: The requirement for how the detail view is presented changes from a dialog to a bottom sheet. The scenarios referencing "detail dialog" need updating. All existing functional requirements (load, copy, share, history expansion) remain in scope with the same behavior.

## Impact

- **UI layer (`AppDetailScreen.kt` only)**: `AlertDialog` replaced with `ModalBottomSheet`. Header layout restructured (no `title` slot in bottom sheets). Close `IconButton` added. History expansion switched from `rememberSaveable` to `remember`.
- **No navigation or ViewModel changes**: Visibility is already driven by the Navigation 3 back stack (`AppDetail` entry). No state flags exist to change.
- **Spec**: `openspec/specs/certificate-inspection/spec.md` — all references to "dialog" and "detail view" replaced with "bottom sheet".
- **No breaking API changes** — no public interfaces, no data layer changes.
