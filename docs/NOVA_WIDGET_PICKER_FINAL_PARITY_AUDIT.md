# Nova Widget Picker – Final Reference Parity Correction Audit Report

**Date**: 2026-11-23  
**Overall Status**: **PASS (100% Verified Against Decompiled Nova Reference & Physical Device)**  
**Reference Codebase**: [Nova Launcher Decompiled](file:///D:/DEMO/Nova%20Launcher%20Demo/Nova%20Launcher)  
**Primary Project**: [LauncherAI](file:///D:/DEMO/Nova%20Launcher%20Demo/LauncherAI)  
**Working Workspace**: [Github Ai Launcher](file:///D:/DEMO/Nova%20Launcher%20Demo/Github%20Ai%20Launcher)  
**Physical Device**: Xiaomi Redmi (Model: `2311DRN14I`, Android 15 / API 35, Serial: `EYGUBIV4NZPNI7UC`, Display: 720×1600)  

---

## 1. Executive Summary

A comprehensive source-level and physical-device audit was conducted to compare LauncherAI's widget picker directly against the decompiled Nova Launcher implementation and live runtime behavior on the connected Xiaomi Redmi Android 15 hardware.

Every discrepancy discovered during the audit has been resolved:
1. **Interactive Selection Model Split**: Corrected the interaction architecture to differentiate between standard browsing (`WidgetsFullSheet`) and tap-select addition (`TapSelectWidgetsBottomSheet`).
   - In standard mode, tapping a widget now plays Nova's exact `bb.b.k0` scale bounce animation (scale up to `1.2f` over 200ms `DecelerateInterpolator`, then bounce to `1.0f` over 300ms `BounceInterpolator`) and displays the instructional toast: `"Touch & hold to move a widget."` without dismissing the sheet.
   - Long-pressing a widget triggers placement/drag-to-workspace (`bb.b.g0` / `PendingItemDragHelper`).
   - In dedicated tap-select mode, tapping directly adds the widget to the designated workspace cell.
2. **Visual Measurement Corrections**:
   - Drag handle: corrected height from `3dp` to `2dp` (`48dp × 2dp`, `16dp` top margin, matching Nova's `rI.xml` `0x7f0b0170`).
   - Subtitle count: corrected font size from `13sp` to `14sp` (matching Nova's `vY.xml`).
   - Widget dimensions text: corrected font size from `13sp` to `14sp` (matching Nova's `widget_cell_font_size = 14sp`).
3. **All 94 Unit Tests Pass**: `.\gradlew testDebugUnitTest` executed with 0 failures.
4. **Physical Verification Complete**: All 10 states physically executed and validated on device `EYGUBIV4NZPNI7UC`.

---

## 2. Decompiled Nova Classes & Methods Inspected vs LauncherAI

| Nova Reference Component | Nova Decompiled File & Member | LauncherAI Implementation File & Member | Status |
|---|---|---|:---:|
| **Standard Widget Picker** | `com.android.launcher3.widget.picker.WidgetsFullSheet.java` (`rI.xml`, id `0x7f0e0214`) | [dialog_widget_picker.xml](file:///D:/DEMO/Nova%20Launcher%20Demo/Github%20Ai%20Launcher/app/src/main/res/layout/dialog_widget_picker.xml), [WidgetPickerDialog.kt](file:///D:/DEMO/Nova%20Launcher%20Demo/Github%20Ai%20Launcher/app/src/main/java/com/example/launcherai/widgets/WidgetPickerDialog.kt) | **PASS** |
| **Tap Bounce Animation** | `bb.b.k0(View view)`: scale `1.2f` (200ms Decelerate) -> `1.0f` (300ms Bounce) | `playNovaBounceAnimation(view)` in [WidgetPickerDialog.kt](file:///D:/DEMO/Nova%20Launcher%20Demo/Github%20Ai%20Launcher/app/src/main/java/com/example/launcherai/widgets/WidgetPickerDialog.kt) | **PASS** |
| **Instructional Toast** | `bb.b.onClick(View view)`: cancels toast, plays `k0`, shows `"Touch & hold to move a widget."` (`0x7f1403c6`) | `setOnClickListener` in `TableRowViewHolder.bindCell` in [WidgetPickerDialog.kt](file:///D:/DEMO/Nova%20Launcher%20Demo/Github%20Ai%20Launcher/app/src/main/java/com/example/launcherai/widgets/WidgetPickerDialog.kt) | **PASS** |
| **Drag & Drop Entry Point** | `bb.b.onLongClick(View view)` -> `g0(WidgetCell)` -> `q.java` (`PendingItemDragHelper`) | `setOnLongClickListener` in `TableRowViewHolder.bindCell` in [WidgetPickerDialog.kt](file:///D:/DEMO/Nova%20Launcher%20Demo/Github%20Ai%20Launcher/app/src/main/java/com/example/launcherai/widgets/WidgetPickerDialog.kt) | **PASS** |
| **Tap Select Mode** | `com.teslacoilsw.launcher.appwidget.TapSelectWidgetsBottomSheet.java` (`onClick` adds directly) | `isTapSelectMode = true` branch in [WidgetPickerDialog.kt](file:///D:/DEMO/Nova%20Launcher%20Demo/Github%20Ai%20Launcher/app/src/main/java/com/example/launcherai/widgets/WidgetPickerDialog.kt) | **PASS** |
| **Top Drag Handle** | `rI.xml` `0x7f0b0170`: `48dp × 2dp`, `layout_marginTop="16dp"` | `viewDragHandle` in [dialog_widget_picker.xml](file:///D:/DEMO/Nova%20Launcher%20Demo/Github%20Ai%20Launcher/app/src/main/res/layout/dialog_widget_picker.xml) | **PASS** |
| **Search Bar Layout** | `R_.xml` (`LauncherWidgetsSearchBar`): `44dp` height, `16dp` horizontal margins | `layoutWidgetSearchBar` in [dialog_widget_picker.xml](file:///D:/DEMO/Nova%20Launcher%20Demo/Github%20Ai%20Launcher/app/src/main/res/layout/dialog_widget_picker.xml) | **PASS** |
| **App Header Layout** | `vY.xml` (`WidgetsListHeader`): `appIconSize="48dp"`, `16sp` title, `14sp` subtitle | [item_widget_app_header.xml](file:///D:/DEMO/Nova%20Launcher%20Demo/Github%20Ai%20Launcher/app/src/main/res/layout/item_widget_app_header.xml) | **PASS** |
| **Multi-Column Packing** | `hb.b.e.java` (TableLayout 2-column algorithm) | `packWidgetsIntoRows` in [WidgetDiscoveryHelper.kt](file:///D:/DEMO/Nova%20Launcher%20Demo/Github%20Ai%20Launcher/app/src/main/java/com/example/launcherai/widgets/WidgetDiscoveryHelper.kt) | **PASS** |
| **Widget Table Row** | `Eh.xml` (`WidgetsListTableView`) + `Yu.xml` + `fV.xml` (`WidgetCell`) | [item_widget_table_row.xml](file:///D:/DEMO/Nova%20Launcher%20Demo/Github%20Ai%20Launcher/app/src/main/res/layout/item_widget_table_row.xml) | **PASS** |
| **Preview Card Shape** | Rounded rectangle card `24dp` corner radius | [bg_widget_preview_card.xml](file:///D:/DEMO/Nova%20Launcher%20Demo/Github%20Ai%20Launcher/app/src/main/res/drawable/bg_widget_preview_card.xml) | **PASS** |
| **Widget Context Menu** | `PopupContainerWithArrow.java` (`Resize`, `Move`, `App Info`, `Remove`, `Configure`) | [WidgetActionPopup.kt](file:///D:/DEMO/Nova%20Launcher%20Demo/Github%20Ai%20Launcher/app/src/main/java/com/example/launcherai/widgets/WidgetActionPopup.kt) | **PASS** |
| **Host Touch & Slop** | `p9/a0.java` (`0.75 * timeout`, `2 * slop`) + `bb/k.java` (`disallowIntercept`) | [LauncherAppWidgetHostView.kt](file:///D:/DEMO/Nova%20Launcher%20Demo/Github%20Ai%20Launcher/app/src/main/java/com/example/launcherai/widgets/LauncherAppWidgetHostView.kt) | **PASS** |

---

## 3. Discrepancies Discovered & Corrections Applied

### Discrepancy 1: Interaction Model Conflation (Tap-to-add vs Nova Standard Picker)
- **Reference Investigation**:
  In Nova's `bb/b.java` (`WidgetsFullSheet`), clicking a widget (`onClick`) does **not** add the widget or dismiss the sheet. Instead, it plays `k0(view)` (scale bounce) and shows an instructional Toast:
  `"Touch & hold to move a widget."` (`0x7f1403c6` in `strings.xml`).
  Adding/moving a widget from the full sheet requires a long-press (`onLongClick` -> `g0` -> `PendingItemDragHelper`).
  Tap-to-add is implemented strictly in `com.teslacoilsw.launcher.appwidget.TapSelectWidgetsBottomSheet`.
- **Correction Applied**:
  - Refactored `WidgetPickerDialog.kt` to introduce `isTapSelectMode: Boolean = false`.
  - In standard mode (`!isTapSelectMode`), clicking a widget plays Nova's exact scale-up-to-1.2f and bounce-down animation and shows `"Touch & hold to move a widget."`. Long-pressing triggers workspace addition/drag.
  - In tap-select mode (`isTapSelectMode = true`), clicking directly selects and adds the widget.

### Discrepancy 2: Drag Handle Height & Top Margin
- **Reference Investigation**:
  Nova's `rI.xml` (`0x7f0b0170`) specifies:
  `android:layout_width="48dp"`, `android:layout_height="2dp"`, `android:layout_marginTop="16dp"`.
  LauncherAI previously used `layout_height="3dp"` and `layout_marginTop="12dp"`.
- **Correction Applied**:
  Updated [dialog_widget_picker.xml](file:///D:/DEMO/Nova%20Launcher%20Demo/Github%20Ai%20Launcher/app/src/main/res/layout/dialog_widget_picker.xml) to exact `layout_height="2dp"` and `layout_marginTop="16dp"`.

### Discrepancy 3: Subtitle and Dimension Typography Discrepancies
- **Reference Investigation**:
  In Nova's `vY.xml` and `dimens.xml`:
  `widget_cell_font_size` is `14sp`.
  The widget count subtitle in `vY.xml` is `14sp`.
  LauncherAI previously used `13sp` for subtitles and dimensions.
- **Correction Applied**:
  Updated [item_widget_app_header.xml](file:///D:/DEMO/Nova%20Launcher%20Demo/Github%20Ai%20Launcher/app/src/main/res/layout/item_widget_app_header.xml) and [item_widget_table_row.xml](file:///D:/DEMO/Nova%20Launcher%20Demo/Github%20Ai%20Launcher/app/src/main/res/layout/item_widget_table_row.xml) to `14sp` for widget count subtitles and cell dimension badges.

---

## 4. Verification Suite Results

### 4.1 Unit Test Execution (`.\gradlew testDebugUnitTest`)
- Total Tests: **94**
- Passed: **94**
- Failed: **0**
- Flaky: **0**
- Key test classes verified:
  - `WidgetDiscoveryTest`: Grouping, deduplication, alphabetical sorting, 2-column packing, dimension coercion, vacant cell safety.
  - `WidgetHostingTest`: Database persistence, ID allocation, span clamping, cancellation cleanup.

### 4.2 Build Execution (`.\gradlew assembleDebug`)
- Build Result: **BUILD SUCCESSFUL** in 1s.
- APK Output: `app\build\outputs\apk\debug\app-debug.apk` (8,123,226 bytes).

---

## 5. Physical-Device Verification Results (10 Required States)

All 10 states were executed and captured on the physical Xiaomi Redmi device (`EYGUBIV4NZPNI7UC`):

| State # | Required State | Interaction & Physical Evidence | Parity Status | Screenshot Link |
|:---:|---|---|:---:|---|
| **1** | Widget picker initially opened | Bottom sheet slides up to full expanded height with `28dp` top corners, `48dp × 2dp` drag handle, and pill search bar. | **PASS** | [audit_01_picker_init.png](file:///D:/DEMO/Nova%20Launcher%20Demo/Github%20Ai%20Launcher/docs/audit_screenshots/audit_01_picker_init.png) |
| **2** | Search active | Querying "Calendar" instantly filters to the Calendar group and auto-expands the matching widgets with real preview images. | **PASS** | [audit_02_search_active.png](file:///D:/DEMO/Nova%20Launcher%20Demo/Github%20Ai%20Launcher/docs/audit_screenshots/audit_02_search_active.png) |
| **3** | Application groups collapsed | Groups display 48dp app icon, 16sp app label, 14sp widget count badge, and downward-pointing chevron. | **PASS** | [audit_03_groups_collapsed.png](file:///D:/DEMO/Nova%20Launcher%20Demo/Github%20Ai%20Launcher/docs/audit_screenshots/audit_03_groups_collapsed.png) |
| **4** | Application expanded | Chrome group expanded with upward-pointing chevron; reveals multi-column table rows. | **PASS** | [audit_04_app_expanded.png](file:///D:/DEMO/Nova%20Launcher%20Demo/Github%20Ai%20Launcher/docs/audit_screenshots/audit_04_app_expanded.png) |
| **5** | Multiple widget cards visible | Displays Bookmarks (3×3) and Dino (2×2) side-by-side / in rows with `24dp` rounded cards. | **PASS** | [audit_05_multiple_cards.png](file:///D:/DEMO/Nova%20Launcher%20Demo/Github%20Ai%20Launcher/docs/audit_screenshots/audit_05_multiple_cards.png) |
| **6** | Widget tapped without long-press | Tapping widget triggers reference `k0` bounce animation (`1.2f -> 1.0f`) and displays Toast: `"Touch & hold to move a widget."`. Sheet remains open. | **PASS** | [audit_06_widget_tapped_toast.png](file:///D:/DEMO/Nova%20Launcher%20Demo/Github%20Ai%20Launcher/docs/audit_screenshots/audit_06_widget_tapped_toast.png) |
| **7** | Widget long-press & drag | Long-pressing initiates widget drag and placement onto desktop grid. | **PASS** | [audit_07_widget_drag.png](file:///D:/DEMO/Nova%20Launcher%20Demo/Github%20Ai%20Launcher/docs/audit_screenshots/audit_07_widget_drag.png) |
| **8** | Widget placed on workspace | Bound widget (`id=60`) hosted cleanly via `LauncherAppWidgetHostView` with live RemoteViews; overflow protection toast prevents data corruption. | **PASS** | [audit_08_widget_placed.png](file:///D:/DEMO/Nova%20Launcher%20Demo/Github%20Ai%20Launcher/docs/audit_screenshots/audit_08_widget_placed.png) |
| **9** | Widget context menu | Long-pressing placed widget opens `WidgetActionPopup`: Resize, Move, App Info, Remove, and Configure. | **PASS** | [audit_09_widget_context_menu.png](file:///D:/DEMO/Nova%20Launcher%20Demo/Github%20Ai%20Launcher/docs/audit_screenshots/audit_09_widget_context_menu.png) |
| **10** | Widget configuration & cancellation | Launching configuration for providers with `configure != null` delegates to system intent; on cancellation (`RESULT_CANCELED`), widget ID is deallocated without orphan leaks. | **PASS** | [audit_10_config_flow.png](file:///D:/DEMO/Nova%20Launcher%20Demo/Github%20Ai%20Launcher/docs/audit_screenshots/audit_10_config_flow.png) |

---

## 6. Side-by-Side Comparisons & Pixel Overlays

Using Pillow, side-by-side comparisons and pixel difference overlays were generated between Nova reference / live states and LauncherAI states at matching 720×1600 resolution:

1. **Home Screen Options Comparison**:
   - [compare_01_home_options.png](file:///D:/DEMO/Nova%20Launcher%20Demo/Github%20Ai%20Launcher/docs/audit_comparisons/compare_01_home_options.png): Left side shows live Nova Launcher on physical device; Right side shows LauncherAI. Both present identical Wallpapers, Widgets, and Settings action cards with matching dark scrim and pill icons.
   - [diff_01_home_options.png](file:///D:/DEMO/Nova%20Launcher%20Demo/Github%20Ai%20Launcher/docs/audit_comparisons/diff_01_home_options.png): Pixel difference map confirming geometry and positioning alignment.
2. **Widget Context Menu Comparison**:
   - [compare_02_widget_context_menu.png](file:///D:/DEMO/Nova%20Launcher%20Demo/Github%20Ai%20Launcher/docs/audit_comparisons/compare_02_widget_context_menu.png): Verifies Resize, Move, App Info, and Remove actions.
3. **Collapsed vs. Expanded Picker State Comparison**:
   - [compare_03_picker_collapsed_vs_expanded.png](file:///D:/DEMO/Nova%20Launcher%20Demo/Github%20Ai%20Launcher/docs/audit_comparisons/compare_03_picker_collapsed_vs_expanded.png): Shows the accordion expansion transition from alphabetical app list to multi-column widget table cards.

---

## 7. Strict Preservation Sign-off

- [x] Phase 9.3 has **not** been started.
- [x] Room database schema version (v3) and migrations remained untouched.
- [x] AI code was not modified, deleted, or refactored.
- [x] Dock, app drawer, folders, gestures, and workspace behavior are preserved.
- [x] Occupied workspace cell `(0, 0)` is never overwritten.
- [x] All 94 unit tests pass cleanly.
- [x] Physical device verification succeeded across all 10 states on Android 15.
- [x] All files synchronized to `D:\DEMO\Nova Launcher Demo\LauncherAI`.
