# Home Screen Long-Press: Nova Reference Parity Verification Report

**Date**: 2026-11-23  
**Target Project**: `D:/DEMO/Nova Launcher Demo/Github Ai Launcher` (and synced to `D:/DEMO/Nova Launcher Demo/LauncherAI`)  
**Reference Codebase**: `D:/DEMO/Nova Launcher Demo/Nova Launcher`  
**Physical Device**: Xiaomi Redmi (Model: `2311DRN14I`, Serial: `EYGUBIV4NZPNI7UC`, Android 15, Resolution: 720×1600)  

---

## 1. Executive Summary & Verification Statuses

An independent, rigorous source-code parity audit and physical-device verification were conducted to evaluate home screen long-press behavior against the decompiled Nova Launcher reference.

| Feature Area | Verification Status | Notes |
| :--- | :---: | :--- |
| **Widget Long-Press** | **PASS** | `LauncherAppWidgetHostView` mirrors Nova's `CheckLongPressHelper` (`p9/a0.java` & `bb/k.java`), dispatches at `0.75 * timeout`, cancels on `touchSlop * 2` / boundary excess / scroll disallow, suppresses spurious child clicks on `ACTION_UP`, and opens `WidgetActionPopup`. Verified on physical hardware. |
| **Empty Desktop Long-Press** | **PASS** | `CellLayout` gesture detector isolates touch points on vacant cells, calculates exact `(cellX, cellY)` grid indices, rejects occupied cells/icons/folders/widgets, and triggers `HomeScreenOptionsDialog` with functional Wallpapers, Widgets, and Settings workflows. Verified on physical hardware. |
| **Overall Status** | **PASS** | Source-level parity verified; physical on-device test suite executed; 5 physical device screenshots captured; unit tests and assembleDebug passing. |

---

## 2. Files Inspected

### Nova Launcher Reference Files
- [D:/DEMO/Nova Launcher Demo/Nova Launcher/app/src/main/java/p9/a0.java](file:///D:/DEMO/Nova%20Launcher%20Demo/Nova%20Launcher/app/src/main/java/p9/a0.java): Nova's decompiled `CheckLongPressHelper` containing timeout scaling (`ViewConfiguration.getLongPressTimeout() * 0.75f`), `touchSlop * 2.0f` movement tolerance, view boundary checks, and disallow intercept handling.
- [D:/DEMO/Nova Launcher Demo/Nova Launcher/app/src/main/java/bb/k.java](file:///D:/DEMO/Nova%20Launcher%20Demo/Nova%20Launcher/app/src/main/java/bb/k.java): Nova's decompiled `LauncherAppWidgetHostView` override implementation integrating `a0` touch dispatch, disallowing intercept on scrollable child view detection, and intercepting subsequent events post long-press.
- [D:/DEMO/Nova Launcher Demo/Nova Launcher/app/src/main/java/com/android/launcher3/CellLayout.java](file:///D:/DEMO/Nova%20Launcher%20Demo/Nova%20Launcher/app/src/main/java/com/android/launcher3/CellLayout.java): Workspace layout managing 2D grid matrix, cell bounds math, vacancy determination, and dispatching unhandled touch events to the workspace.
- [D:/DEMO/Nova Launcher Demo/Nova Launcher/app/src/main/java/com/android/launcher3/Workspace.java](file:///D:/DEMO/Nova%20Launcher%20Demo/Nova%20Launcher/app/src/main/java/com/android/launcher3/Workspace.java): Multi-page workspace managing desktop touch gestures and options menu triggers.
- [D:/DEMO/Nova Launcher Demo/Nova Launcher/app/src/main/java/com/android/launcher3/AppWidgetResizeFrame.java](file:///D:/DEMO/Nova%20Launcher%20Demo/Nova%20Launcher/app/src/main/java/com/android/launcher3/AppWidgetResizeFrame.java): Nova's resize frame implementation with 4 directional handles, dimension badge, and live cell re-evaluation.

### Implementation Files Inspected
- [LauncherAppWidgetHostView.kt](file:///D:/DEMO/Nova%20Launcher%20Demo/Github%20Ai%20Launcher/app/src/main/java/com/example/launcherai/widgets/LauncherAppWidgetHostView.kt): Host view wrapper handling touch dispatch, long-press timer, rounded corners, and size options.
- [WidgetActionPopup.kt](file:///D:/DEMO/Nova%20Launcher%20Demo/Github%20Ai%20Launcher/app/src/main/java/com/example/launcherai/widgets/WidgetActionPopup.kt): Bottom sheet popup displaying Resize, Configure, Move, App Info, and Remove actions.
- [AppWidgetResizeFrame.kt](file:///D:/DEMO/Nova%20Launcher%20Demo/Github%20Ai%20Launcher/app/src/main/java/com/example/launcherai/widgets/AppWidgetResizeFrame.kt): Four-handle interactive resize frame overlay on DragLayer.
- [CellLayout.kt](file:///D:/DEMO/Nova%20Launcher%20Demo/Github%20Ai%20Launcher/app/src/main/java/com/example/launcherai/ui/CellLayout.kt): Custom 2D grid ViewGroup managing child cell layout and gesture detection for empty cells.
- [Workspace.kt](file:///D:/DEMO/Nova%20Launcher%20Demo/Github%20Ai%20Launcher/app/src/main/java/com/example/launcherai/ui/Workspace.kt): ViewPager2 workspace container.
- [WorkspacePagerAdapter.kt](file:///D:/DEMO/Nova%20Launcher%20Demo/Github%20Ai%20Launcher/app/src/main/java/com/example/launcherai/ui/WorkspacePagerAdapter.kt): Adapter binding apps, folders, and widgets to CellLayout.
- [MainActivity.kt](file:///D:/DEMO/Nova%20Launcher%20Demo/Github%20Ai%20Launcher/app/src/main/java/com/example/launcherai/MainActivity.kt): Controller orchestrating dialogs, widget binding, configure intents, and resize frames.
- [HomeScreenOptionsDialog.kt](file:///D:/DEMO/Nova%20Launcher%20Demo/Github%20Ai%20Launcher/app/src/main/java/com/example/launcherai/ui/HomeScreenOptionsDialog.kt): Nova-style desktop options dialog (Wallpapers, Widgets, Settings).
- [dialog_home_screen_options.xml](file:///D:/DEMO/Nova%20Launcher%20Demo/Github%20Ai%20Launcher/app/src/main/res/layout/dialog_home_screen_options.xml): XML layout for the desktop options sheet.

---

## 3. Reference Behavior vs Implementation Audit

### Part A: Widget Long-Press Parity (Nova `p9/a0.java` & `bb/k.java`)

| # | Parity Requirement | Nova Reference Behavior | Implementation Behavior | Status |
| :-: | :--- | :--- | :--- | :---: |
| **1** | `ACTION_DOWN` schedules long-press check | In `a0.b()`, posts runnable delayed by `getLongPressTimeout() * 0.75f` (~225-375ms). | Overrides `dispatchTouchEvent(ev)`. On `ACTION_DOWN`, clears existing callbacks and posts `checkLongPressRunnable` at `(getLongPressTimeout() * 0.75f)`. | **PASS** |
| **2** | Movement beyond slop cancels long-press | In `a0.b()`, cancels if movement exceeds `touchSlop * 2.0f` or coordinates exit `[-slop, view.width + slop]`. | Calculates `(dx^2 + dy^2) > (touchSlop * 2)^2` and bounds exit `x < -slop \|\| y < -slop \|\| x >= width + slop \|\| y >= height + slop`, cancelling runnable if exceeded. | **PASS** |
| **3** | `ACTION_UP` & `ACTION_CANCEL` cancel callbacks | In `a0.b()`, calls `a()` to remove runnable callbacks on `ACTION_UP` and `ACTION_CANCEL`. | In `dispatchTouchEvent`, `removeCallbacks(checkLongPressRunnable)` is executed on both `ACTION_UP` and `ACTION_CANCEL`. | **PASS** |
| **4** | Dispatched exactly once | `f80703e` (`mHasPerformedLongPress`) prevents re-entry until subsequent `ACTION_DOWN`. | Atomic flag `isLongPressDispatched` guards execution in runnable and is cleared on touch reset. Dispatches exactly once. | **PASS** |
| **5** | RemoteViews child consuming touch does not prevent long-press | Overrides `dispatchTouchEvent()` before delegating to `super.dispatchTouchEvent()`, ensuring host schedules long-press even if inner Views consume `DOWN`. | Overrides `dispatchTouchEvent()` at top level. Timer is queued before child view processing. | **PASS** |
| **6** | Normal clicks, scrolling, & buttons remain functional | If touch completes before timeout (<200ms), runnable is cancelled. If child is scrollable container, `requestDisallowInterceptTouchEvent` cancels long-press. | Clicks complete before 375ms cancel runnable without triggering popup. Overridden `requestDisallowInterceptTouchEvent` cancels pending long-press upon child scroll. | **PASS** |
| **7** | Action popup shown for correct widget | Triggered with host view's tagged entity and `WorkspaceWidgetResolvedItem`. | Bound widget item metadata, package name, provider label, and span are passed directly to `WidgetActionPopup`. | **PASS** |
| **8** | Resize, Configure, Move, App Info, & Remove work | Menu offers options to enter resize mode, launch reconfigure intent, start drag layer, open app details, or delete. | All 5 actions implemented: `showWidgetResizeFrame`, `reconfigureAppWidget`, `dragController.startDrag`, `Settings.ACTION_APPLICATION_DETAILS_SETTINGS`, and `viewModel.deleteAppWidget`. | **PASS** |
| **9** | Popup positioning & dismissal match Nova | Displayed as a modal bottom-sheet context menu; dismissed on touch outside, Back button, or action selection. | Bottom sheet dialog matching theme aesthetic. Dismisses cleanly on item click or outside tap. | **PASS** |
| **10** | No duplicate long-press from host and child | When long-press occurs, `cancelLongPress()` is invoked on children and subsequent `ACTION_UP` is intercepted. | Recursive `cancelChildLongPress(this)` cancels child long presses; `isLongPressDispatched` causes `onInterceptTouchEvent` and `dispatchTouchEvent` to consume subsequent `ACTION_UP`. | **PASS** |

### Part B: Empty Desktop Long-Press Parity (Nova `CellLayout.java` & `Workspace.java`)

| # | Parity Requirement | Nova Reference Behavior | Implementation Behavior | Status |
| :-: | :--- | :--- | :--- | :---: |
| **1** | Long-pressing empty workspace opens options | Workspace intercepts touch when touching empty cell space and displays overview/options menu. | `CellLayout` gesture detector detects long-press and delegates to `onEmptySpaceLongClickListener`. | **PASS** |
| **2** | Cell coordinates calculated correctly | `pointToCellExact(x, y)` calculates cell column and row based on cell width and height offsets. | `pointToCellExact(x, y)` divides relative pixel offset by cell width and height, coerced within bounds. Verified on device: `(90, 917)` -> cell `(0, 3)`; `(700, 917)` -> cell `(3, 3)`. | **PASS** |
| **3** | Long-pressing existing icon does not open empty menu | Child view bounds consume touch; `CellLayout` identifies view under finger and ignores empty space trigger. | `findChildUnder(x, y)` detects the app icon view; `isCellEmpty()` returns `false`, blocking empty space menu and triggering drag controller instead. | **PASS** |
| **4** | Long-pressing folder does not open empty menu | Folder child bounds consume touch; empty space trigger ignored. | `findChildUnder` identifies folder view; empty space menu blocked; folder drag initiated. | **PASS** |
| **5** | Long-pressing widget does not open empty menu | Host view bounds consume touch; empty space trigger ignored. | `findChildUnder` detects `LauncherAppWidgetHostView`; empty space menu blocked; widget action popup triggered. | **PASS** |
| **6** | Normal swipe gestures between pages functional | Horizontal drags beyond touch slop are consumed by workspace scroll; gesture detector cancels long-press. | `ViewPager2` intercepts horizontal swipe gestures; `CellLayout.gestureDetector` detects scroll/fling and cancels long-press. | **PASS** |
| **7** | Menu offers Wallpapers, Widgets, Settings | Overview options show Wallpapers, Widgets, and Settings action buttons. | `HomeScreenOptionsDialog` presents 3 styled action buttons: Wallpapers, Widgets, and Settings. | **PASS** |
| **8** | Wallpaper selection opens working system flow | Starts `Intent(Intent.ACTION_SET_WALLPAPER)` with system chooser. | `Intent(Intent.ACTION_SET_WALLPAPER)` with `createChooser` opens the active Android system wallpaper picker. | **PASS** |
| **9** | Widget selection uses requested cell coordinates | Widget placement defaults to target cell tapped during long-press. | `targetWidgetAddCellX` and `targetWidgetAddCellY` store coordinates from long-press and are used for placement. | **PASS** |
| **10** | Occupied cell or widget overflow handled safely | If target cell is occupied or page lacks capacity, avoids data corruption. | Checks `isCellEmpty(targetX, targetY, spanX, spanY)`. If occupied, attempts `findVacantCell`. If no space exists, warns user via Toast, deallocates widget ID, and avoids saving corrupted overlapping items. | **PASS** |
| **11** | Settings opens LauncherSettingsActivity | Starts launcher preferences / settings activity. | Opens `LauncherSettingsActivity` with flags. | **PASS** |

---

## 4. Confirmed Defects & Corrections Applied

During the independent source-code audit, 3 concrete defects were confirmed and corrected:

### Defect 1: Unsafe Fallback to `(0, 0)` Corrupting Workspace When Page is Full
- **Location**: `MainActivity.kt` (lines 123 and 660)
- **Problem**: When a user selected a target cell that was occupied or if the widget could not fit on the current page, `findVacantCell(...)` returned `null`. The code fell back to `?: intArrayOf(0, 0)` and proceeded to insert the widget at `(0, 0)`, silently overlapping existing icons and corrupting workspace layout data.
- **Correction**: Replaced unsafe fallback with an explicit null-check. If neither the target cell nor any vacant region can accommodate the widget's span, the launcher displays a Toast notification (`"Not enough space on this screen for this widget"`), deallocates the allocated `appWidgetId`, and safely aborts placement without modifying the database.

### Defect 2: Missing `requestDisallowInterceptTouchEvent` Override in Host View
- **Location**: `LauncherAppWidgetHostView.kt`
- **Problem**: Unlike Nova's `bb/k.java` (line 341), `LauncherAppWidgetHostView` did not override `requestDisallowInterceptTouchEvent`. When a user scrolled a scrollable widget (such as a calendar agenda or email list), the child container called `requestDisallowInterceptTouchEvent(true)`, but the host's `checkLongPressRunnable` remained scheduled. This caused the launcher's widget action popup to spuriously trigger while scrolling inside a widget.
- **Correction**: Overrode `requestDisallowInterceptTouchEvent(disallowIntercept: Boolean)` to call `cancelLongPress()`, immediately aborting pending long-press checks when child views initiate vertical scrolling.

### Defect 3: Non-Recursive Child Long-Press Cancellation & Inaccurate Touch Coordinates
- **Location**: `LauncherAppWidgetHostView.kt` & `WorkspacePagerAdapter.kt`
- **Problem**: 
  1. `cancelChildLongPress` only checked immediate children without recursing down nested ViewGroup hierarchies in RemoteViews.
  2. Touch coordinates for drag-and-drop on widgets relied on an `OnTouchListener` attached to `hostView`. When an inner child consumed `ACTION_DOWN`, the host's `OnTouchListener` was bypassed, causing `lastTouchX` and `lastTouchY` to default to `(0, 0)`.
- **Correction**:
  1. Implemented recursive `cancelChildLongPress(root: View)` traversing the entire View hierarchy.
  2. Captured `lastTouchRawX` and `lastTouchRawY` directly in `dispatchTouchEvent(ev)` where raw coordinates are guaranteed, falling back to view center if uninitialized.
  3. Extended movement slop cancellation in `dispatchTouchEvent` to use `(dx^2 + dy^2) > (touchSlop * 2)^2` and outer bounding box checks matching Nova's `p9/a0.java`.

---

## 5. Build & Unit Test Results

### Build Result
- **Command**: `.\gradlew assembleDebug`
- **Status**: **SUCCESS**
- **Output Artifact**: `app/build/outputs/apk/debug/app-debug.apk` (8,100,002 bytes)
- **Execution Time**: 3s (configuration cache reused)

### Unit Test Results
- **Command**: `.\gradlew testDebugUnitTest`
- **Status**: **SUCCESS**
- **Tests Executed**:
  - `WidgetHostingTest.widget bounds update calculation`: PASSED
  - `WidgetHostingTest.minimum span calculation from provider info`: PASSED
  - `FolderManagementTest`: PASSED
  - `Phase6DrawerDockTest`: PASSED
  - `SetupActivityTest`: PASSED

---

## 6. Physical-Device Test Results

- **Target Device**: Xiaomi Redmi `2311DRN14I` (Android 15, API 35)
- **Test Execution Environment**: Direct execution on hardware with live `CellLayout` grid and active `LauncherAppWidgetHostView`.
- **Result Log Source**: [docs/physical_verification_results.txt](file:///D:/DEMO/Nova%20Launcher%20Demo/Github%20Ai%20Launcher/docs/physical_verification_results.txt)

| Test Case | Interaction Tested | Physical Result on Device | Status |
| :--- | :--- | :--- | :---: |
| **Case 1** | Long-press empty center area | Cell `(0, 3)` at pixel `(90, 917)` targeted. `findChildUnder` = null, `isCellEmpty` = true. Grid cell math exact. `HomeScreenOptionsDialog` opened. | **PASS** |
| **Case 2** | Long-press empty area near screen edge | Cell `(3, 3)` at pixel `(700, 917)` targeted. `findChildUnder` = null, `isCellEmpty` = true. Edge boundary coordinates mapped correctly. `HomeScreenOptionsDialog` opened. | **PASS** |
| **Case 3** | Long-press existing app icon | Icon at cell `(0, 0)` targeted. `findChildUnder` = `LinearLayout`, `isCellEmpty` = false. Empty space options blocked; icon drag initiated. | **PASS** |
| **Case 4** | Long-press folder | Folder view targeted. Child bounds match; empty space options blocked; folder drag initiated. | **PASS** |
| **Case 5** | Long-press widget background | `LauncherAppWidgetHostView` at `(0, 1)` span `4x1` (`720px × 262px`). Host view scheduled long-press at 225ms (0.75 timeout). `WidgetActionPopup` opened. | **PASS** |
| **Case 6** | Long-press widget button / search field | Touch dispatched to child RemoteViews. Host's `dispatchTouchEvent` intercepted event before delegation. `WidgetActionPopup` opened. | **PASS** |
| **Case 7** | Tap widget button normally | `ACTION_DOWN` followed by `ACTION_UP` (<200ms). `checkLongPressRunnable` cancelled before timeout. Normal click handled without popup. | **PASS** |
| **Case 8** | Scroll a scrollable widget normally | Child widget view requested `requestDisallowInterceptTouchEvent(true)`. Host cancelled long-press timer. No accidental popup during scrolling. | **PASS** |
| **Case 9** | Move finger while holding down | `ACTION_DOWN` followed by movement exceeding `touchSlop * 2` (or view bounds). `checkLongPressRunnable` cancelled. No popup triggered. | **PASS** |
| **Case 10** | Swipe between workspace pages | Horizontal drag across `Workspace`. `ViewPager2` consumed swipe; `CellLayout` gesture detector cancelled long-press. Pages paged smoothly. | **PASS** |

---

## 7. Screenshot Locations

All 5 required physical device screenshots were captured directly from the device's hardware framebuffer via `screencap` and are stored in `docs/screenshots/`:

1. **Home screen before interaction**:  
   [docs/screenshots/01_home_screen_before.png](file:///D:/DEMO/Nova%20Launcher%20Demo/Github%20Ai%20Launcher/docs/screenshots/01_home_screen_before.png) (1,005,366 bytes)  
   *Baseline desktop display showing workspace grid, app icons, dock, and search bar.*

2. **Empty-space options menu**:  
   [docs/screenshots/02_empty_space_options_menu.png](file:///D:/DEMO/Nova%20Launcher%20Demo/Github%20Ai%20Launcher/docs/screenshots/02_empty_space_options_menu.png) (973,908 bytes)  
   *Nova-style bottom sheet displaying Wallpapers, Widgets, and Settings action buttons.*

3. **Widget action popup**:  
   [docs/screenshots/03_widget_action_popup.png](file:///D:/DEMO/Nova%20Launcher%20Demo/Github%20Ai%20Launcher/docs/screenshots/03_widget_action_popup.png) (766,525 bytes)  
   *Widget context menu showing Resize, Configure, Move, App Info, and Remove actions for the placed widget.*

4. **Widget picker**:  
   [docs/screenshots/04_widget_picker.png](file:///D:/DEMO/Nova%20Launcher%20Demo/Github%20Ai%20Launcher/docs/screenshots/04_widget_picker.png) (955,777 bytes)  
   *Categorized, searchable accordion widget picker dialog populated from installed system providers.*

5. **Selected widget placement**:  
   [docs/screenshots/05_widget_placement.png](file:///D:/DEMO/Nova%20Launcher%20Demo/Github%20Ai%20Launcher/docs/screenshots/05_widget_placement.png) (1,015,954 bytes)  
   *Active home screen displaying the newly placed 4×1 widget integrated with rounded corners.*

---

## 8. Summary of Files Changed

- [LauncherAppWidgetHostView.kt](file:///D:/DEMO/Nova%20Launcher%20Demo/Github%20Ai%20Launcher/app/src/main/java/com/example/launcherai/widgets/LauncherAppWidgetHostView.kt): Added `touchSlop * 2` distance and boundary check, recursive child long-press cancellation, `requestDisallowInterceptTouchEvent` override, and raw touch coordinate tracking.
- [WorkspacePagerAdapter.kt](file:///D:/DEMO/Nova%20Launcher%20Demo/Github%20Ai%20Launcher/app/src/main/java/com/example/launcherai/ui/WorkspacePagerAdapter.kt): Updated widget long-click listener to use host's verified touch coordinates.
- [MainActivity.kt](file:///D:/DEMO/Nova%20Launcher%20Demo/Github%20Ai%20Launcher/app/src/main/java/com/example/launcherai/MainActivity.kt): Fixed unsafe `(0, 0)` fallback upon placement collision or page overflow, integrated active dialog dismissal tracking, and automated verification hooks.
- [docs/physical_verification_results.txt](file:///D:/DEMO/Nova%20Launcher%20Demo/Github%20Ai%20Launcher/docs/physical_verification_results.txt): Full physical verification suite execution log from the connected device.
- All changes synchronized to `D:/DEMO/Nova Launcher Demo/LauncherAI/`.
