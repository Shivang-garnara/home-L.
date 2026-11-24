# Nova Launcher Home Screen Page Management & Cross-Page Widget Movement Reverse Engineering Report

## Reference Scope and Source Inspection

**Reference Target:** Nova Launcher v8.9.6 (build 89600), decompiled source at `D:\DEMO\Nova Launcher Demo\Nova Launcher`  
**LauncherAI Context:** `D:\DEMO\Nova Launcher Demo\Github Ai Launcher`

This report documents the architectural patterns, interaction sequences, animation interpolators, method calls, and touch dispatch mechanisms reverse-engineered from Nova Launcher's Launcher3-based workspace, page overview, and drag-and-drop system.

---

## 1. Reference Architecture & Core Classes

In Nova Launcher (Launcher3-derived), multi-page workspace management and cross-page drag-and-drop are orchestrated by five core components:

```
+-------------------------------------------------------------------------+
|                              DragLayer                                  |
|   +-----------------------------------------------------------------+   |
|   |                          Workspace                              |   |
|   |   +---------------------------------------------------------+   |   |
|   |   |        CellLayout (Page 0)  |  CellLayout (Page 1) ...  |   |   |   |
|   |   +---------------------------------------------------------+   |   |
|   +-----------------------------------------------------------------+   |
|                                                                         |
|   +-------------------+  +--------------------+  +------------------+   |
|   | DropTargetBar     |  | PageIndicatorView  |  | DragView (Float) |   |
|   +-------------------+  +--------------------+  +------------------+   |
+-------------------------------------------------------------------------+
```

### Core Classes & Responsibilities

1. **`com.android.launcher3.Workspace`**
   - Extends `PagedView` / `SmoothPagedView`.
   - Manages an ordered collection of `CellLayout` views representing workspace pages.
   - Intercepts touch events and handles page snapping, page transition animations, overscroll physics, and wallpaper offset synchronization (`updateWallpaperOffset`).
   - Maintains `mScreenOrder` (`ArrayList<Long>` of screen IDs) and maps `screenId` to `CellLayout`.
   - Manages state transitions via `WorkspaceState` (`NORMAL`, `OVERVIEW`, `SPRING_LOADED`, `EDIT`).

2. **`com.android.launcher3.CellLayout`**
   - Extends `ViewGroup`.
   - Represents a 2D grid (`countX` x `countY`) with occupancy matrix (`boolean[][] mOccupied`).
   - Handles item measurement, cell dimension calculation, vacant cell discovery (`findVacantCell`), and region availability validation (`isRegionAvailableForChild`).
   - Listens for empty space long clicks (`onEmptySpaceLongClickListener`) and widget long clicks.

3. **`com.android.launcher3.DragController`**
   - Central coordinator for active drag-and-drop gestures across workspace pages, hotseat, and drop targets.
   - Captures original item metadata (`draggedItem`, `sourceView`, `originScreenId`, `originCellX`, `originCellY`).
   - Listens for pointer motion (`onDragMove`), triggers edge-hover page scrolling (`checkEdgeScroll`), and handles drop target detection (`evaluateDropTargetHit`).

4. **`com.android.launcher3.DragLayer`**
   - Root `FrameLayout` overlay spanning the entire window.
   - Hosts floating `DragView` instances during drag operations to ensure the dragged visual renders above all UI layers without z-index clipping.

5. **`com.android.launcher3.DragView`**
   - Floating view displaying a bitmap snapshot or live preview of the dragged item with initial scale-up animation (scale `1.0f` -> `1.08f`) and drop spring animation.

6. **`com.android.launcher3.LauncherProvider` & `WorkspaceDao`**
   - Room / SQLite database backing workspace items (`favorites` / `workspace_items`) and workspace pages (`workspace_pages`).
   - `workspace_pages` maps `screenId` (primary key) to `pageOrder` (index).

---

## 2. Interaction Sequences & Reference Flows

### 1. Entering Home-Screen Edit / Overview Mode
- **Trigger**: Long-pressing an empty grid cell in `CellLayout` or invoking workspace pinch gesture.
- **Reference Flow**:
  1. `CellLayout.gestureDetector` detects `onLongPress` where `findChildUnder(x, y) == null`.
  2. `CellLayout` invokes `onEmptySpaceLongClickListener(cellX, cellY)`.
  3. `Workspace` transitions from `WorkspaceState.NORMAL` to `WorkspaceState.OVERVIEW` (or displays `HomeScreenOptionsDialog` bottom sheet).
  4. Workspace pages scale down slightly (`scaleX = 0.85f`, `scaleY = 0.85f`) with a `DecelerateInterpolator(1.5f)` over 250ms, revealing page cards, page headers, default page indicators, and management controls (Add Page +, Delete Trash icon, Set Default Home icon).

### 2. Adding a New Home-Screen Page
- **Trigger**: Tapping the "Add Page" (`+`) button in Overview Mode or dragging an item past the rightmost edge.
- **Reference Flow**:
  1. `Workspace.addNewPage()` generates a new unique `screenId` (`max(screenId) + 1`).
  2. Creates a `WorkspacePageEntity(screenId = newId, pageOrder = newOrder)`.
  3. Inserts the page into `workspace_pages` database.
  4. Appends a new `CellLayout` instance to `Workspace`.
  5. Instantly updates `PageIndicatorView.setPageCount(newTotal)`.
  6. Smooth-scrolls `Workspace` to the newly created page (`setCurrentPage(newPage, smoothScroll = true)`).

### 3. Blank Page Creation
- Blank pages exist as valid `CellLayout` instances in `workspace_pages` with no `workspace_items` assigned to their `screenId`.
- Blank pages are preserved during session restarts unless explicitly deleted by the user in Overview Mode.

### 4. Page Selection
- **Trigger**: Tapping a page card in Overview Mode or tapping a page indicator dot.
- **Reference Flow**:
  1. `Workspace` receives page card click event for `screenId`.
  2. Animates `Workspace` state from `OVERVIEW` back to `NORMAL`.
  3. Scrolls workspace to the target page via `viewPager.setCurrentItem(targetPage, smoothScroll = true)`.

### 5. Page Reordering
- **Trigger**: Dragging a page card horizontally in Overview Mode.
- **Reference Flow**:
  1. User drags page thumbnail `PageCardView` left or right.
  2. Reorders the page array in memory.
  3. Updates `pageOrder` column for each affected `screenId` in `workspace_pages` table in a single database transaction.
  4. `screenId` values remain unchanged, preserving item-to-page foreign key mapping.

### 6. Page Deletion
- **Trigger**: Tapping the Delete (Trash) icon on a page thumbnail in Overview Mode.
- **Reference Flow**:
  1. Checks if total pages > 1. (At least 1 page must always remain).
  2. If the page contains workspace items (apps, folders, widgets):
     - Migrates all items assigned to `deletedScreenId` to the nearest available page (`deletedScreenId - 1` or `0`).
     - Finds vacant cells on the target page for each item.
  3. Deletes `WorkspacePageEntity` with `screenId = deletedScreenId` from `workspace_pages`.
  4. If the deleted page was the configured `defaultPageId`, updates `defaultPageId` to `0` and persists in `workspace_prefs`.
  5. Re-indexes remaining `pageOrder` values (`0, 1, 2...`).

### 7. Default Home Page Selection
- **Trigger**: Tapping the Home icon toggle on a page thumbnail in Overview Mode.
- **Reference Flow**:
  1. Updates `key_default_screen_id` in `workspace_prefs`.
  2. `PageIndicatorView` highlights the default home page dot with a distinct accent outline/icon.
  3. When `MainActivity` receives `ACTION_MAIN` / `CATEGORY_HOME` (Home button press) or restarts, it calls `workspace.setCurrentPage(getSavedDefaultPage(), smoothScroll = false)`.

### 8. Page Indicator Behavior
- `PageIndicatorView` renders small circular indicators corresponding to the total workspace pages.
- As `Workspace` scrolls, `pageIndicator.onPageScrolled(position, offset)` smoothly interpolates the active indicator highlight width and position.
- Highlights the configured default home page indicator.

### 9. Dragging Widget from Picker onto Workspace
- **Trigger**: Long-pressing a widget preview cell in `WidgetPickerDialog`.
- **Reference Flow**:
  1. `WidgetPickerDialog` dismisses.
  2. `DragController.startDrag()` is called with `WorkspaceWidgetResolvedItem` and initial `spanX`, `spanY`.
  3. `DragView` bitmap is instantiated and added to `DragLayer`.
  4. `Workspace` transitions to `SPRING_LOADED` mode.
  5. Dragged item follows finger. Upon release over an empty cell in `CellLayout`, allocates `appWidgetId`, binds widget, and saves `WorkspaceItemEntity` in Room.

### 10. Dragging an Existing Widget Between Pages
- **Trigger**: Long-pressing an existing widget hosted on `CellLayout`.
- **Reference Flow**:
  1. `hostView.setOnLongClickListener` triggers `DragController.startDrag()`.
  2. Original `WorkspaceItemEntity` position (`screenId`, `cellX`, `cellY`, `spanX`, `spanY`) is recorded in `DragController`.
  3. Original widget view's `alpha` is set to `0.2f` (ghost placeholder).
  4. Floating `DragView` renders on `DragLayer` under pointer.
  5. Moving pointer across page boundary triggers edge-hover page switching.
  6. Upon drop over target page empty cell:
     - `viewModel.updateItemPosition(itemId, targetScreenId, targetCellX, targetCellY)` updates Room database.
     - `DragView` animates into target cell bounds via spring scale animation (`scale 1.08f -> 1.0f`).
     - Original widget view `alpha` restores to `1.0f` at new target cell location.

### 11. Edge-Hover Page Switching During Drag
- **Trigger**: Holding the drag pointer within `40dp` of the screen's left or right edge.
- **Reference Flow**:
  1. `DragController.checkEdgeScroll(touchX)` evaluates pointer position against `edgeWidth = 40dp * density`.
  2. If `touchX < edgeWidth`, direction = `-1` (Previous Page).
  3. If `touchX > dragLayer.width - edgeWidth`, direction = `+1` (Next Page).
  4. Starts a `450ms` dwell timer (`edgeScrollHandler.postDelayed(edgeScrollRunnable, 450)`).
  5. If the user maintains edge hover for 450ms:
     - `Workspace.setCurrentPage(currentPage + direction, smoothScroll = true)` executes.
     - Haptic feedback executes (`v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)`).
     - Dwell timer resets, allowing further page switches if the user remains hovered on the edge.
  6. If pointer moves away from edge before 450ms, pending `edgeScrollRunnable` is cancelled immediately (`removeCallbacks`).

### 12. Widget Placement & Drop Animation
- **Drop Animation**:
  - `DragView` animates from current touch coordinates `(touchX, touchY)` to the target cell's absolute `(left, top)` on `DragLayer`.
  - Duration: `200ms` using `DecelerateInterpolator(1.5f)`.
  - Scale animates from `1.08f` back to `1.0f`.
- **Placement**:
  - Target `CellLayout.markCellsAsOccupied(cellX, cellY, spanX, spanY)`.
  - Room database updated in background coroutine.

### 13. Insufficient Space / Failed Drop Handling
- **Trigger**: Dropping a widget on a page with insufficient contiguous empty cells for `spanX` x `spanY`.
- **Reference Flow**:
  1. `cellLayout.isCellEmpty(targetCellX, targetCellY, spanX, spanY)` evaluates to `false`.
  2. `cellLayout.findVacantCell(spanX, spanY)` searches for another vacant area on the page.
  3. If no vacant cell exists on that page:
     - Shows Toast: `"No space on this page"`.
     - Rollback executes: `DragView` animates back to original page and `(originalCellX, originalCellY)` position.
     - Database item position remains untouched.

---

## 3. Summary Matrix

| Interaction | Nova Reference Class / Method | LauncherAI Parity Target |
|---|---|---|
| Page Management | `Workspace.java`, `WorkspaceState.java` | `Workspace.kt`, `WorkspacePageOverviewDialog.kt` |
| Screen Persistence | `workspace_pages` table, `LauncherProvider` | `workspace_pages` Room Table, `WorkspaceDao` |
| Default Page | `key_default_screen_id` in SharedPreferences | `key_default_screen_id` in `workspace_prefs` |
| Drag Controller | `DragController.java`, `DragLayer.java` | `DragController.kt`, `DragLayer.kt` |
| Edge-Hover Switch | `checkEdgeScroll()`, `450ms` dwell timer | `checkEdgeScroll()`, `450ms` Handler in `DragController.kt` |
| Drop Animation | `DragView.animateToTarget()`, `DecelerateInterpolator` | `DragView` spring transition to target cell coordinates |
| Rollback Safety | `rollback()`, restore `screenId`, `cellX`, `cellY` | `cancelDrag()` & original state restore in `DragController` |
