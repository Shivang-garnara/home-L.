# Nova Launcher Widget Picker – Reverse Engineering & Architectural Blueprint

**Reference Project**: `D:\DEMO\Nova Launcher Demo\Nova Launcher`  
**Reference Photos**: `D:\DEMO\Nova Launcher Demo\Photos`  
**Target Project**: `D:\DEMO\Nova Launcher Demo\LauncherAI` (`Github Ai Launcher`)  
**Date**: 2026-11-23  

---

## 1. Executive Summary

This report documents the reverse engineering of the decompiled Nova Launcher widget picker architecture. The investigation covers decompiled Java classes, public resource ID mappings (`public.xml`), layout XML structures, dimensional metrics (`dimens.xml`), drawable assets, and interaction handlers.

The Nova Launcher widget picker is not a generic bottom sheet dialog. It is a specialized, hardware-accelerated sliding view container (`WidgetsFullSheet`) attached directly to Nova's `DragLayer`, backed by a multi-column grouped adapter (`WidgetsRecyclerView` + `WidgetsListTableView`), with real-time search filtering, expandable application group headers (`WidgetsListHeader`), and adaptive widget cells (`WidgetCell`).

---

## 2. Reference Classes & Architecture Inventory

Through cross-referencing inlined decimal resource constants with `public.xml`, the following core classes and components were identified:

| Class Path | Extends / Implements | Primary Responsibility |
|---|---|---|
| `com.android.launcher3.widget.picker.WidgetsFullSheet` | `bb.b`, `h`, `w`, `a` | Main sliding widget picker view attached to `DragLayer`. Controls bottom sheet expansion, search bar attachment, fast scrolling, and spring overscroll animations. |
| `com.teslacoilsw.launcher.appwidget.TapSelectWidgetsBottomSheet` | `com.android.launcher3.widget.WidgetsBottomSheet` | Tap-to-add variant of the widget bottom sheet used for direct widget selection. |
| `com.android.launcher3.views.OptionsPopupView` | `AbstractFloatingView` | Home screen empty-space long-press options menu ("Wallpapers", "Widgets", "Settings"). Method `q0(NovaLauncher)` inflates and displays `WidgetsFullSheet` (`layout: 0x7f0e0214 / 2131624468`). |
| `com.android.launcher3.widget.picker.WidgetsListHeader` | `LinearLayout` | Application group header view displaying the app icon, title, widget count subtitle, and expand/collapse indicator. |
| `com.android.launcher3.widget.picker.WidgetsListTableView` | `TableLayout` | Multi-column table layout container hosting rows of widget preview cells for an expanded application. |
| `com.android.launcher3.widget.WidgetCell` | `LinearLayout` | Individual widget cell displaying the widget preview thumbnail (`WidgetImageView`), widget title, and grid dimensions (`spanX × spanY`). Contains `CheckLongPressHelper` (`a0`). |
| `com.android.launcher3.widget.WidgetCellPreview` | `FrameLayout` | Container framing the widget preview image with rounded corners (`24dp`) and elevation shadow. |
| `com.android.launcher3.widget.WidgetImageView` | `View` | Custom view rendering the scaled widget preview bitmap or app icon fallback. |
| `com.android.launcher3.widget.picker.search.LauncherWidgetsSearchBar` | `LinearLayout` | Search bar view with rounded pill background, search icon, `ExtendedEditText` for real-time querying, and clear button. |
| `fb.n` | `RecyclerView.Adapter` | RecyclerView adapter managing application group headers (`eb.f`) and expanded widget table contents (`eb.d`). |
| `fb.s` | `oa.a` | Header view holder binder for `WidgetsListHeader`. |
| `fb.u` | `oa.a` | Content view holder binder for `WidgetsListTableView`. Implements grid row packing algorithm. |
| `hb.b` | Abstract Utility | Core widget calculation engine: `hb.b.e` implements the row packing algorithm; `hb.b.c` calculates widget options bundle (`appWidgetSizes`, `appWidgetCornerRadius`). |
| `bb.b` | `oz.b` | Base bottom sheet controller implementing `View.OnClickListener` and `View.OnLongClickListener`. Dispatches `g0(WidgetCell)` to start interactive drag-and-drop. |

---

## 3. Resource & Layout Inventory

Decompiled layout references mapped via `public.xml`:

| Layout Name | Hex Resource ID | Decimal Constant | Decompiled File | Root Element |
|---|---|---|---|---|
| `widgets_full_sheet` | `0x7f0e0214` | `2131624468` | `rI.xml` | `com.android.launcher3.widget.picker.WidgetsFullSheet` |
| `widgets_bottom_sheet_content` | `0x7f0e0212` | `2131624466` | `s4.xml` | `<merge>` containing `WidgetsRecyclerView` & `SearchAndRecommendationsView` |
| `widgets_search_bar` | `0x7f0e021b` | `2131624475` | `R_.xml` | `com.android.launcher3.widget.picker.search.LauncherWidgetsSearchBar` |
| `widgets_list_row_header` | `0x7f0e0218` | `2131624472` | `vY.xml` | `com.android.launcher3.widget.picker.WidgetsListHeader` |
| `widgets_table_container` | `0x7f0e021c` | `2131624476` | `Eh.xml` | `com.android.launcher3.widget.picker.WidgetsListTableView` |
| `widget_cell` | `0x7f0e020d` | `2131624461` | `Yu.xml` | `com.android.launcher3.widget.WidgetCell` |
| `widget_cell_content` | `0x7f0e020e` | `2131624462` | `fV.xml` | `<merge>` containing `WidgetCellPreview`, label, & span |
| `tap_select_widgets_bottom_sheet`| `0x7f0e01f8` | `2131624440` | `xI.xml` | `com.teslacoilsw.launcher.appwidget.TapSelectWidgetsBottomSheet` |

---

## 4. Complete Nova Reference Interaction Map (17 Sequence Questions)

### 1. How the user enters the widget picker
- User long-presses empty space on the desktop `CellLayout`.
- `CellLayout.onLongClick` triggers `OptionsPopupView.s0(...)`, presenting options: "Wallpapers", "Widgets", "Settings".
- Tapping "Widgets" dispatches `OptionsPopupView.q0(NovaLauncher)` (`ab.d0(2)`).

### 2. How the bottom sheet opens
- `OptionsPopupView.q0` inflates `widgets_full_sheet` (`0x7f0e0214` / `rI.xml`) into `DragLayer` (`p1Var.P`).
- The sheet animates upward using a spring-interpolated slide transition (`b0(267L, z11)`).
- A dim scrim is rendered behind the sheet (`widgets_picker_scrim` / `0x7f06064e`).
- Top-left and top-right corners have a `28dp` radius (`widget_list_top_bottom_corner_radius`).
- A centered drag handle pill (`48dp × 2dp`, `16dp` marginTop, color `textColorSecondary`) appears at the top.

### 3. How installed widget applications are discovered
- Discovered by querying `AppWidgetManager.getInstalledProvidersForProfile` across all active `UserManager.userProfiles`.
- Filtered to ensure packages are enabled and valid.
- Manifest contains `<queries>` for `android.appwidget.action.APPWIDGET_UPDATE` for Android 11+ and 15 visibility.

### 4. How applications are grouped and sorted
- Grouped by owning package name and user profile into `eb.c` (`eb.f` header and `eb.d` content models).
- Application groups are sorted alphabetically by application label (`ha.h.f55877l`).
- Widgets within each application are sorted by area (`spanX × spanY`) and label.

### 5. How application groups expand and collapse
- Each application header has a toggle arrow checkbox (`CheckBox` with `button="0x7f0806d0"`).
- Tapping a header calls `fb.n.c(isExpanded, g0Var)`:
  - If collapsing: sets `f51419g = null`, updates item animators, and removes the `WidgetsListTableView` row.
  - If expanding: sets `f51419g = g0Var`, expands the `WidgetsListTableView` immediately below the header, and rotates the chevron arrow 180°.
  - Nova logs `LAUNCHER_WIDGETSTRAY_APP_EXPANDED`.

### 6. How individual widgets are displayed
- When an application group expands, its widgets are laid out in a `WidgetsListTableView` (`TableLayout`).
- Packed into rows using `hb.b.e`:
  - Small widgets (1-2 columns wide) are placed side-by-side in 2 columns.
  - Large widgets (3-4 columns wide) take an entire row.
- Each widget cell (`WidgetCell`) has:
  - Top: Preview image container (`WidgetCellPreview`, `24dp` rounded corners).
  - Middle: Widget name (`14sp`, `textColorPrimary`, centered).
  - Bottom: Widget dimensions (`14sp`, `textColorSecondary`, `alpha="0.7"`, format `spanX × spanY`, e.g. `2 × 2`, `4 × 2`).

### 7. How preview images are obtained
- Real preview images loaded via `provider.loadPreviewImage(context, 0)`.
- If unavailable, falls back to `provider.loadIcon(context, 0)` or the owning app icon.
- Rendered inside `WidgetImageView` with `widget_preview_corner_radius` (`24dp`).

### 8. How widget dimensions are displayed
- Cell spans calculated via `Math.ceil((minDp + 30) / 70.0)` clamped to grid bounds.
- Displayed as `spanX × spanY` (e.g., `2 × 2`, `4 × 2`, `3 × 3`) directly underneath the title in `14sp` secondary text.

### 9. What happens when a widget is tapped
- In Nova's `bb.b.onClick(View)`:
  - Performs a scale bounce animation on the cell (`k0(view)`: scale 0.9 then bounce back to 1.0).
  - Displays a toast with text `long_press_widget_to_add` (`0x7f1403c6`: "Touch & hold a widget to move it around the desktop.").
  - In `TapSelectWidgetsBottomSheet`: directly selects and places the widget on the workspace.

### 10. Whether Nova displays a separate widget preview/details screen
- **No separate details activity or dialog exists in Nova Launcher.**
- All details (previews, labels, dimensions, descriptions) are integrated directly into the `WidgetCell` inside the bottom sheet.

### 11. Whether the widget is added using a button, drag-and-drop, or another interaction
- Primary interaction: **Drag-and-Drop** (`onLongClick` -> `g0(WidgetCell)` -> `PendingItemDragHelper` lifts the widget and enters workspace drag mode).
- Direct tap interaction is supported in `TapSelectWidgetsBottomSheet` where single-tap directly adds the widget to a vacant workspace cell.

### 12. How the target workspace location is selected
- In drag-and-drop: user drags the preview over `CellLayout`, and target cell is determined by drop location.
- In tap-to-add: target cell is the requested empty-space long-press cell if available; otherwise, first vacant cell from `CellLayout.findVacantCell(spanX, spanY)`.

### 13. How binding permissions are handled
- `AppWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, provider)`.
- If false, launches system permission dialog via `ACTION_APPWIDGET_BIND`.
- User approval binds the widget; rejection deletes the allocated `appWidgetId`.

### 14. How configuration activities are launched
- If `provider.configure != null`, launches `ACTION_APPWIDGET_CONFIGURE` with `EXTRA_APPWIDGET_ID`.
- If user completes (`RESULT_OK`), widget is placed and displayed.
- If cancelled, allocated `appWidgetId` is immediately cleaned up.

### 15. How widget placement is persisted
- Persisted with `appWidgetId`, `cellX`, `cellY`, `spanX`, `spanY`, `screenId`, and provider component into database.
- Restored upon launcher restart via `AppWidgetHost.createView()`.

### 16. How resizing and removal work
- Long-pressing a placed widget opens the widget action popup (`WidgetActionPopup`), with options: "Resize", "Configure", "Move", "App Info", "Remove".
- Resize displays 4-corner interactive handle overlay (`AppWidgetResizeFrame`) snapped to cell grid boundaries.
- Remove deletes the widget from the workspace and calls `appWidgetHost.deleteAppWidgetId()`.

### 17. How cancellation and placement failures are handled
- If no vacant cell fits the widget, displays an informative toast ("Not enough space on this screen for this widget") and deletes `appWidgetId`—**never overwriting cell (0, 0)**.
- If binding or configuration is cancelled, `deleteAppWidgetId` cleans up allocated resources.

---

## 5. Nova Visual Design Specifications (from Reference Resources)

| UI Element | Reference Specification | Resource Identifier |
|---|---|---|
| **Sheet Corner Radius** | `28dp` (top-left & top-right) | `widget_list_top_bottom_corner_radius` |
| **Sheet Background** | Dark surface `#1E1E1E` / theme surface with scrim | `widgets_picker_scrim` / `0x7f0801bf` |
| **Top Drag Handle** | `48dp` width, `2dp` height, `16dp` marginTop, `textColorSecondary` | `id: 0x7f0b0170` |
| **Search Bar Height** | `44dp` | `layout: widgets_search_bar` |
| **Search Bar Corners** | `22dp` / `24dp` pill radius | `bg_search_bar` |
| **Search Bar Margins** | `16dp` horizontal (`widget_list_horizontal_margin`), `12dp` vertical | `widget_list_horizontal_margin` |
| **Search Text Size** | `16sp`, primary text color | `ExtendedEditText` |
| **App Group Header Padding**| `20dp` vertical, `16dp` horizontal | `widget_list_header_view_vertical_padding` |
| **App Icon Dimensions** | `48dp × 48dp`, marginHorizontal `16dp` | `appIconSize="48dp"` in `vY.xml` |
| **App Group Title** | `16sp`, bold/medium, primary text color | `0x7f0b00b2` |
| **App Group Subtitle** | `14sp`, secondary text color, `alpha="0.7"` ("X widgets") | `0x7f0b00b1` |
| **Expand Arrow Indicator** | 24dp chevron, rotating 0° to 180° | `0x7f0b06bd` |
| **Widget Table Layout** | Multi-column grid: 2 columns for 1-2 span; 1 column for 3-4 span | `WidgetsListTableView` (`Eh.xml`) |
| **Widget Cell Padding** | `16dp` horizontal, `8dp` vertical | `widget_cell_horizontal_padding`, `widget_cell_vertical_padding` |
| **Preview Container Radius**| `24dp` corner radius | `widget_preview_corner_radius` |
| **Preview Margin Vertical**| `8dp` top and bottom | `layout_marginVertical="8dp"` |
| **Widget Title Typography** | `14sp`, `textColorPrimary`, centered, maxLines 1 | `widget_cell_font_size` (`Yu.xml` / `fV.xml`) |
| **Widget Span Typography** | `14sp`, `textColorSecondary`, `alpha="0.7"`, centered (`spanX × spanY`) | `0x7f0b075f` |

---

## 6. LauncherAI Implementation Audit & Parity Gap Analysis

| Functionality | Nova Launcher Reference | Current LauncherAI | Required Correction |
|---|---|---|---|
| **Top Sheet Container** | Pill drag handle (`48dp × 2dp`), `28dp` top corner radius, spring damping. | Generic dialog with "Widgets" header bar and close button. | Replace title/close bar with centered drag handle pill (`48dp × 2dp`) and `28dp` rounded top sheet corners. |
| **Search Bar** | `LauncherWidgetsSearchBar`: 44dp pill, 16sp font, start search icon, clear button on end, 16dp margin. | 44dp search bar present, but with 20dp margins and non-matching hint. | Update margins to `16dp`, set hint to "Search widgets", refine pill radius to `22dp`. |
| **App Group Header** | `48dp` app icon, `20dp` vertical padding, `16sp` app title, `14sp` count subtitle with `0.7` alpha directly below title, chevron arrow on right. | `36dp` app icon, `12dp` padding, chip count badge on right next to chevron. | Update app icon to `48dp`, padding to `16dp` H / `14dp` V, place "X widgets" subtitle directly below title, remove chip badge, and align chevron to far right. |
| **Expanded Widget Layout** | Multi-column `WidgetsListTableView`: 2-column grid for small widgets, 1-column for wide widgets. | Single-column vertical list of flat rows. | Implement the multi-column `WidgetsListTableView` grid layout so small widgets sit side-by-side in pairs, matching Nova! |
| **Widget Preview Presentation** | Prominent preview card with `24dp` corner radius, subtle shadow, centered above title. | Small 72dp thumbnail on the left side of text. | Switch to vertical `WidgetCell` structure: centered preview container (`24dp` corners) on top, centered title (`14sp`), and centered span (`14sp`). |
| **Widget Dimensions Text** | Formatted as `spanX × spanY` (e.g. `2 × 2`, `4 × 2`), clean centered. | Formatted as `4 × 2 (240dp × 48dp)`. | Change format to `spanX × spanY` (`2 × 2`, `4 × 2`), matching Nova reference. |
| **Widget Selection & Add** | Tap triggers bounce animation; long-press drags. Direct tap adds in tap-sheet. | Tap selects and places directly. | Keep instant tap-to-add for convenience while adding visual bounce feedback and drag support. |
| **Binding & Config Lifecycle** | Strict check: if bind fails -> permission intent; if config cancels -> delete ID; vacant cell safety. | Implemented and verified in `MainActivity.kt`. | Preserve existing verified lifecycle. |
| **Empty State & Loading** | Centered empty text `0x7f0b0462`, fast scroller popup. | Loading spinner and empty search state present. | Refine typography to `20sp` sans-serif-medium matching Nova. |

---

## 7. Next Action Plan (Phase C)

1. **Reconstruct Layout XMLs**:
   - Update `dialog_widget_picker.xml` to match `widgets_full_sheet` (`rI.xml` + `s4.xml`): add top centered drag handle pill, update background to `28dp` top corners, adjust search bar.
   - Update `item_widget_app_header.xml` to match `widgets_list_row_header` (`vY.xml`): `48dp` app icon, `16sp` title, `14sp` subtitle with `alpha="0.7"`, right-aligned chevron.
   - Create `item_widget_table_row.xml` and `item_widget_cell.xml` to match `widgets_table_container` (`Eh.xml`) and `widget_cell` (`Yu.xml` + `fV.xml`): 2-column multi-widget table rows, large `24dp` rounded preview cards, centered title, centered `spanX × spanY` dimensions.
2. **Update `WidgetPickerDialog.kt` Adapter**:
   - Implement row packing (`hb.b.e` parity): group widgets within each expanded app into multi-column table rows.
   - Render `WidgetCell`s with authentic preview imagery, bounce pressed state, and tap-to-add / long-press drag support.
3. **Verify on Physical Device**:
   - Run unit tests (`testDebugUnitTest`).
   - Build and deploy to Xiaomi Redmi Android 15.
   - Capture all 8 comparison screenshots.
   - Write `docs/NOVA_WIDGET_PICKER_IMPLEMENTATION_AND_VERIFICATION.md`.
