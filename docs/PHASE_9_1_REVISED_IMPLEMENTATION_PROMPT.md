# Phase 9.1 — Revised Implementation Prompt

## Objective

Implement reliable workspace grid configuration in LauncherAI. Preserve its existing Kotlin, Room, MVVM, `ViewPager2`, and drag-and-drop architecture. Treat the supplied Nova Launcher v8.9.6 screenshots as the visual reference only for what they actually show. In particular, the screenshots show the Nova settings hierarchy and Home screen settings entry but **do not show the grid settings detail screen**. Do not claim an exact visual match to controls that are not evidenced.

Read `docs/PHASE_9_1_REFERENCE_SCREENSHOT_ANALYSIS.md` before implementation. It separates screenshot evidence, source evidence, and unknowns.

## Required behavior

1. **One authoritative persisted grid configuration.** Establish one source of truth for workspace column and row counts. Settings UI, ViewModels/repositories, `MainActivity`, the workspace adapter, widget placement, and any existing grid-setting action (including the AI tool if it continues to be supported) must read and write through the same configuration path. Avoid independently maintained in-memory or preference copies that can diverge. Preserve existing architecture and stored user layouts. Do not change Room schemas or migrations for this phase; select a compatible existing persistence mechanism unless an approved later scope changes that constraint.
2. **Uniform dimensions.** Every current and future `CellLayout` on every `ViewPager2` workspace page must use the authoritative dimensions. A grid update must reach attached and subsequently created/rebound page holders, including pages that were off-screen when the update occurred. Keep the page indicator and current page behavior intact.
3. **Position preservation.** A grid change must preserve every workspace app shortcut, folder, and widget's page and cell position whenever its existing footprint can fit. Do not silently move items only in view memory. Persist any required position/span changes together through the repository so rendered and stored layouts agree. Do not alter folder-child membership or rank as a side effect.
4. **Out-of-bounds and collision policy.** Before applying a new grid, evaluate all desktop placements, including widget spans, against its bounds and occupancy. Keep valid non-colliding placements. For invalid placements, apply one deterministic, documented recovery policy that keeps every item if space exists (for example, search for the nearest available valid cell with stable tie-breaking); do not overwrite/delete an item or silently accept a collision. If the new grid cannot contain all items, retain the prior valid configuration and placements and surface a clear user-facing failure. Ensure the chosen positions are persisted before publishing the new configuration to the UI.
5. **Widget span validation.** Validate that spans are positive, fit within the selected grid, and do not overlap other items before insert, update, reflow, or rendering. Derive provider minimum spans using the currently selected grid where possible; clamp or reject impossible spans with a clear outcome. Do not assume a fixed 4x5 grid. This phase does not request widget resizing UI unless required to preserve existing supported flows.
6. **Immediate UI update.** Once a configuration change is accepted and persisted, refresh all workspace pages without requiring activity recreation or app restart. Keep ViewPager2 paging, page selection, item click, folder behavior, widget hosting, and current drag/drop paths working. During drag, use the active grid dimensions for coordinate conversion and collision checks. Ensure the transient app-from-drawer placement path inserts a new persistent item when that existing flow is involved in validating this grid behavior.
7. **Restart persistence.** On next process/activity startup, load the authoritative grid and the matching persisted placements before rendering pages. The selected dimensions and valid item/widget positions must survive restart. Preserve existing installs and default workspace contents.
8. **Settings UI.** Keep grid controls reachable from the Home screen settings entry. Align the settings presentation with screenshot-supported Nova cues: light surface, brown/dark text and icons, pale search/control surfaces, clear section/row hierarchy, and concise Home screen/grid labels. The supplied screenshots show a “Home screen” row described as “Set grid, icon layout, dock settings, and more”; they do not expose grid rows/columns choices, control type, preview, or dialog styling. Reuse the current supported grid choices unless product review supplies a different specification. Do not invent Nova-specific grid controls or state exact pixel parity. Clearly show the active columns × rows and reflect a successful change immediately.

## Architecture and preservation constraints

- Use current Kotlin, Room, repository, ViewModel and ViewPager2 patterns.
- Keep item/folder/widget position data in the current `WorkspaceItemEntity` representation unless a separate approved task expands the schema.
- Do not modify Room schema versions or migrations, Gradle dependencies, AI chat/provider functionality, or unrelated launcher phases.
- Do not remove or rename existing project files or user-facing flows.
- Preserve drag/drop, page indicator, widget host lifecycle, folder membership, and current page selection.
- Avoid relying on UI-only occupancy adjustments as a substitute for persisted layout changes.

## Acceptance criteria

- Changing grid dimensions updates every page, including pages created after the change.
- Existing valid shortcuts, folders, and widgets remain at their same cells and pages.
- Invalid/colliding placements are handled deterministically, persisted, and never lost; a change that cannot fit all items is rejected without partially applying.
- Widget spans are checked against actual selected grid bounds and occupancy.
- Settings and all other grid consumers show/use the same value.
- The drawer-to-workspace placement path (if retained as part of the exercised grid flow) creates a Room row rather than attempting to update a transient id.
- ViewPager2 swiping, page indicator, drag/drop, folder interactions, widget hosting, and item launches remain functional.
- Returning from settings updates the visible workspace immediately; restarting retains the chosen grid and layout.
- UI uses screenshot-supported reference details without fabricating details from the absent Nova grid settings screenshot.
- No AI provider/chat code, unrelated phases, schemas/migrations, dependencies, or unrelated layouts are changed.

## Required implementation summary

After implementation, report the authoritative persistence path, the deterministic placement recovery rule, how attached/offscreen pages receive updates, how widget spans are validated, and what was checked for restart/drag-and-drop compatibility. If exact Nova grid-detail visual matching is still required, request the missing grid-settings reference screenshot or a written control specification before asserting visual parity.
