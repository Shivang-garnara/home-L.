# Phase 9.2 — Data-Driven Dock (Hotseat) Integration Prompt

## Objective

Implement a data-driven, configurable home screen Dock (Hotseat) in LauncherAI. Replace the static 5-button XML dock layout in `activity_main.xml` with a dynamic, Room DB-bound view that reacts to `WorkspaceViewModel.resolvedDockItems` and `key_dock_capacity` settings. Preserve existing Kotlin, Room, MVVM, `ViewPager2`, drag-and-drop, and AI architecture.

Read `docs/PHASE_9_1_INDEPENDENT_VERIFICATION_REPORT.md` and `docs/NOVA_LAUNCHER_FEATURE_AND_UI_ROADMAP.md` before implementation.

---

## Background and Evidence

1. **Screenshot Reference**: Screenshot #10 (`Screenshot_2026-11-18-10-19-01-046_com.teslacoilsw.launcher.jpg`) shows a 5-icon dock row at the bottom of the home screen featuring phone, messaging, app drawer affordance, browser, and camera shortcuts. Screenshot #12 includes a Home screen settings entry for dock configuration.
2. **Current Source State**:
   - Room DB represents dock items via `container = WorkspaceItemEntity.CONTAINER_DOCK` (-101).
   - `WorkspaceDao` provides `getWorkspaceItemsByContainerFlow(CONTAINER_DOCK)`.
   - `WorkspaceRepository` provides `dockItemsFlow` and populates default dock items.
   - `WorkspaceViewModel` exposes `resolvedDockItems: StateFlow<List<WorkspaceItemDisplay>>` and `updateDockItemPosition(...)`.
   - `LauncherSettingsActivity` allows choosing 3, 5, or 7 icon slots and writes `key_dock_capacity` to `workspace_prefs`.
   - **Current Gap**: `MainActivity.kt` and `activity_main.xml` use static hardcoded XML ImageViews (`dockPhone`, `dockMessages`, `dockAppDrawer`, `dockBrowser`, `dockCamera`) and ignore `resolvedDockItems` and `key_dock_capacity`. `DragController.kt` does not treat the dock as an active drop target.

---

## Required Behavior

1. **Data-Driven Dock View**:
   - Replace the static XML layout in `activity_main.xml` with a dynamic, data-bound Dock layout view (such as a single-row `CellLayout` or dedicated `DockLayout`) pinned at the bottom of `MainActivity`.
   - Collect `WorkspaceViewModel.resolvedDockItems` reactively in `MainActivity` and render the active shortcuts, app icons (supporting icon packs), labels, and folders present in Room database container `-101`.
   - Ensure the App Drawer launcher affordance/button remains available and functional in the dock.

2. **Configurable Dock Capacity**:
   - Read and respect `workspace_prefs` key `key_dock_capacity` (options: 3, 5, or 7 icon slots; default 5).
   - Dynamically compute cell widths so 3, 5, or 7 items are evenly spaced across the dock width.
   - When dock capacity is modified in `LauncherSettingsActivity`, update the visible dock layout immediately without requiring activity recreation.
   - **Capacity Overflow Policy**: If current dock items exceed the newly selected capacity (e.g. changing from 7 to 3 slots), reflow overflow items safely to available vacant spots on the workspace desktop pages via `WorkspaceRepository` so no items are lost or silently deleted.

3. **Drag-and-Drop Integration**:
   - Extend `DragController` to register the Dock as a valid drag source and drop target.
   - Support the following drop interactions:
     a) Dragging an app or folder from the workspace or app drawer into an empty or occupied dock slot.
     b) Reordering items within the dock by dragging between dock slots (`cellX` / `rank`).
     c) Dragging an item out of the dock onto a workspace desktop cell or drop target (Remove / App Info / Uninstall).
     d) Dropping a workspace or drawer app onto an existing dock app to create a folder in the dock, or adding to an existing dock folder.

4. **Restart Persistence**:
   - All dock item positions (`cellX`/`rank`), folder memberships, and capacity selections must survive application process restarts.
   - Default dock items initialized on first run must remain consistent.

5. **Icon Pack & Theme Resolution**:
   - Render dock app shortcuts using the active icon pack or system drawables resolved by `WorkspaceViewModel`.

---

## Architecture and Preservation Constraints

- Preserve existing Kotlin, Room schema (v3), MVVM, `ViewPager2`, and `WorkspaceGridConfig` architecture.
- Do not modify Room schema versions or migrations.
- Do not remove or alter any AI provider (`GeminiAiProvider`, `ClaudeAiProvider`, `OpenAiProvider`, `LocalAgentProvider`), AI settings, AI tools, or dependencies.
- Do not implement unrelated launcher phases (e.g. folder window styles, drawer fast-scroll indexing, or icon shape masks).

---

## Acceptance Criteria

- Dock items render dynamically from Room database container `-101`.
- Changing dock capacity in settings (3, 5, 7 slots) updates the visible dock slots immediately.
- Dragging apps/folders between workspace, app drawer, and dock updates Room database records and UI cleanly.
- Items overflowing reduced dock capacity are safely moved to vacant workspace desktop cells without data loss.
- Dock layout and capacity choices survive app cold restarts.
- Unit tests written for dock data-binding, capacity changes, item reordering, overflow reflow, and restart persistence.
- All unit tests pass (`:app:testDebugUnitTest`) and debug build succeeds (`:app:assembleDebug`).

---

## Required Summary Output

After implementation, report:
1. The dock data-binding architecture and view structure used in `MainActivity`.
2. How dock capacity changes and overflow reflow are handled.
3. How `DragController` handles dock drop targets and reordering.
4. Unit test and build results.
5. Unresolved issues and physical-device verification details.
