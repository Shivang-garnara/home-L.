# Phase 9.1 Reference Screenshot Analysis

## Scope and evidence

This document records visual evidence from the 13 user-supplied screenshots, current LauncherAI source inspection, and a comparison relevant to Phase 9.1 (workspace grid configuration). Screenshots were viewed directly at their supplied paths in `D:\DEMO\Nova Launcher Demo\Photos`. No source, resource, schema, migration, or build files were changed. No device runtime session beyond the screenshots was available.

The screenshot sequence does **not** include a Home screen grid settings detail screen, a workspace grid overlay, or a widget placement/resize interaction. Such details are marked **UNKNOWN** rather than inferred from Nova's general feature set. The screenshots establish visible UI and some apparent interaction sequence; they do not establish all destinations or behavior.

## Screenshot-by-screenshot observations

| # | File / screen | Visible hierarchy, controls, layout and style | Interaction / destination evidence |
|---|---|---|---|
| 1 | `Screenshot_2026-11-18-10-17-59-607_com.teslacoilsw.launcher.jpg` — Welcome | Full-screen dark abstract wallpaper; centered Nova mark, “Welcome to Nova Launcher”, orange “Get started” pill; bottom “Restore backup” and “Skip setup” actions. Status and Android navigation bars visible. White large sans-serif title, warm orange accent. | “Get started”, restore, and skip are visible. Subsequent images suggest setup proceeds to launcher selection/onboarding, but the exact transition from this tap is not proven by the still. |
| 2 | `Screenshot_2026-11-18-10-18-07-525_com.google.android.permissioncontroller.jpg` — Default home app chooser | Android system rounded light panel over dimmed onboarding background. Heading “Set Nova Launcher as your default home app?” followed by Home Launcher, LauncherAI, Nova Launcher, and System launcher rows with icons/radio controls; System launcher is current default. Cancel and disabled “Set as default” actions. | System chooser to select Nova and set default; Cancel. This is Android UI, not Nova's own permission screen. |
| 3 | `Screenshot_2026-11-18-10-18-17-427_com.teslacoilsw.launcher.jpg` — Plan selection | Dark illustrated onboarding with close X; Nova mark; heading “Your home screen, perfected” / “Choose the plan that fits you”. Scrollable cards for Nova Prime and Nova Plus, prices, features, radio pricing choices, “Try 3 days free”; orange/brown accents and thin outlined cards. Lower card extends below viewport. | Close and trial/subscription controls are visible. Exact selected purchase flow/destination is not shown. |
| 4 | `Screenshot_2026-11-18-10-18-21-547_com.teslacoilsw.launcher.jpg` — Search setup | Dark onboarding page with back arrow, progress bar, “Search” heading. “Mode” dropdown set to “Immersive”; Shortcut Search toggle on with explanatory copy; Contact Search toggle off with copy. “Home screen” and “App drawer” subsections each have search-bar-placement dropdowns (shown as “Dock (Below icons)” and “Top”). Orange outlined controls and a bottom-right next arrow. | Back, dropdowns, toggles and next arrow indicate setup choices. Destination beyond the next page is not established here. |
| 5 | `Screenshot_2026-11-18-10-18-38-682_com.teslacoilsw.launcher.jpg` — App drawer setup | Same dark onboarding frame, back arrow/progress and heading “App drawer”. “App drawer style” dropdown set to “Vertical”. Toggles shown on: “Show Keyboard”, “Frequently used apps”, “Frequently used shortcuts”, and “Smart Folders”, each with descriptive copy. | Back and next arrow. These options are not grid settings. |
| 6 | `Screenshot_2026-11-18-10-18-42-852_com.teslacoilsw.launcher.jpg` — Icon style setup | Heading “Icon style”; four bright color swatches; “Icon size” at 125%, minus/plus controls and slider; shapes “Rounded square (Default)”, “Round”, “Squircle”, “Flower”; “More” option; next arrow. | Icon sizing, shape selection, more-shapes action, back and next. |
| 7 | `Screenshot_2026-11-18-10-18-45-375_com.teslacoilsw.launcher.jpg` — More shapes dialog | Light lavender rounded modal titled “More shapes” overlays dimmed icon-style page. A grid of shape choices: Square, Teardrop, Pebble, Vessel, Pentagon, Hexagon (two visibly distinct hexagon glyphs), Heptagon, Octagon, Crop. | Shape choice and modal dismissal implied; resulting selected shape is not discernible. |
| 8 | `Screenshot_2026-11-18-10-18-51-077_com.teslacoilsw.launcher.jpg` — Setup complete | Dark onboarding wallpaper, back arrow and circled check; large “All done!”, centered sparkle and “Setup is finished”; wide orange “Go home” button at bottom. | “Go home” exits setup; following images show chooser and home, but exact event order may include another system prompt. |
| 9 | `Screenshot_2026-11-18-10-18-55-306_com.google.android.permissioncontroller.jpg` — Default home app chooser (second appearance) | Android chooser over visible home screen. Same title/options as #2, System launcher current default, added “Don’t ask again” checkbox, Cancel, disabled “Set as default”. | Choosing Nova and confirming would establish the default home app. The screenshot still shows System launcher selected, so Nova has not yet been made default in this captured state. |
| 10 | `Screenshot_2026-11-18-10-19-01-046_com.teslacoilsw.launcher.jpg` — Home screen | Android home wallpaper fills the screen. One Nova Settings shortcut at top-right; Gmail and Play Store at mid-lower left/right; five-icon dock row; pale rounded search bar near bottom with app-grid/search glyph left and Assistant dots right. Status/navigation bars visible. No visible page dots in this capture. | App shortcuts, dock apps and search affordance visible. No swipe or page transition is shown; placement spans/settings cannot be established. |
| 11 | `Screenshot_2026-11-18-10-19-08-010_com.teslacoilsw.launcher.jpg` — App drawer | Light background; rounded “Search apps” field with drawer/search glyph; “Nova Settings” chip; centered “No Frequent Apps” empty state; thin horizontal divider; category/app icons and labels (Entertainment, Games, AI Photo Vid…, App Name, etc.); multi-column app list; right-side scroll thumb; system bars. | Search, category group and app launch affordances are visible; scroll position shown. Folder, shortcut placement and widget interactions are not shown. |
| 12 | `Screenshot_2026-11-18-10-19-16-823_com.teslacoilsw.launcher.jpg` — Nova settings main list | Light settings surface with “Nova Launcher” heading, Search field, red outlined warning “Not set as default / Nova is not the default launcher. Tap to set.” List rows: Nova AI, Home screen (“Set grid, icon layout, dock settings, and more”), App drawer (“Set layout, style, opening gesture, and more”), Folders (“Set window styles, background colors, and icon layout”). Bottom navigation has Settings selected and Style. Brown text/icons on pale background. | Rows imply navigation to those setting areas; Home screen is the only supplied screenshot evidence that grid settings sit under Home screen. Tap destinations/screens are not shown. |
| 13 | `Screenshot_2026-11-18-10-19-20-893_com.teslacoilsw.launcher.jpg` — Nova settings lower list | Same light settings shell and Search field. Rows: Search, Cards, Look & feel, Gestures & inputs, Feed page, Integrations, and disabled-looking Notification badges with Prime badge; lower content clipped by viewport. Bottom Settings/Style navigation. | Settings destinations implied by rows; no grid control shown. |

## Confirmed screenshot evidence by requested topic

| Topic | Evidence from screenshots | Status |
|---|---|---|
| First launch/onboarding | Welcome, default-home chooser, subscription plan, Search setup, App drawer setup, Icon style, More shapes dialog, setup-complete screen | Confirmed visually; some sequence edges are inferred from filenames/timestamps and not guaranteed |
| Default launcher selection | Android HOME chooser lists Nova, LauncherAI and other homes; System launcher is selected/current in both captures | Confirmed visually; Nova default selection is not shown |
| Permissions | No runtime permission request is shown. The Home role/default chooser is a system selection UI, not a runtime permission dialog. Contact Search's explanatory text mentions granting Contacts permission, but no grant dialog is shown. | Permission flows otherwise unknown |
| Home/workspace | Wallpaper, one top-right shortcut, two spaced app icons, five-slot dock, bottom search bar | Confirmed visible snapshot; grid dimensions and other workspace pages unknown |
| Workspace grid settings / rows and columns | Settings main list has a Home screen row whose subtitle mentions grid. No detail screen, grid preview, row/column controls, limits, defaults, or confirmation behavior appears. | Detail unknown |
| Page indicators/navigation | No clear workspace page indicator or page swipe is visible in home screenshot. Onboarding screens show back/progress and next arrows. | Workspace page count/navigation unknown from screenshots |
| Folders, shortcuts, widgets | Folder settings category and “shortcut search” onboarding are mentioned. No folder opened, shortcut placed, widget picker/placement/resize control is visible. | Placement behavior and UI unknown |
| Dialogs, sheets, menus | Android chooser (#2, #9), subscription cards, More shapes modal (#7), onboarding dropdowns/toggles. No workspace context menu, widget sheet, or grid dialog is supplied. | Those interactions unknown |
| Colors, type, icons | Onboarding uses dark wallpaper, white sans-serif typography, orange/coral accents and outlined controls; settings/home/drawer use light backgrounds, brown text/icons and pale pink search surfaces; Android dialogs follow system styling. | Screenshot-specific appearance confirmed; exact design tokens/measurements not established |

## Source inspection and current implementation findings

The roadmap `docs/NOVA_LAUNCHER_FEATURE_AND_UI_ROADMAP.md` already records a source audit. The following source paths were read directly for this Phase 9.1 review:

- `app/src/main/java/com/example/launcherai/MainActivity.kt`
- `app/src/main/java/com/example/launcherai/ui/Workspace.kt`
- `app/src/main/java/com/example/launcherai/ui/WorkspacePagerAdapter.kt`
- `app/src/main/java/com/example/launcherai/ui/CellLayout.kt`
- `app/src/main/java/com/example/launcherai/ui/WorkspaceViewModel.kt`
- `app/src/main/java/com/example/launcherai/ui/PageIndicatorView.kt`
- `app/src/main/java/com/example/launcherai/ui/LauncherSettingsActivity.kt`
- `app/src/main/java/com/example/launcherai/data/repository/WorkspaceRepository.kt`
- `app/src/main/java/com/example/launcherai/data/db/WorkspaceDao.kt`
- `app/src/main/java/com/example/launcherai/data/db/WorkspaceItemEntity.kt`
- `app/src/main/java/com/example/launcherai/data/db/WorkspacePageEntity.kt`
- `app/src/main/java/com/example/launcherai/data/db/LauncherDatabase.kt`
- `app/src/main/java/com/example/launcherai/dragndrop/DragController.kt`
- `app/src/main/java/com/example/launcherai/widgets/WidgetPickerDialog.kt`
- `app/src/main/java/com/example/launcherai/ai/tools/impl/ModifyWorkspaceGridTool.kt`
- `app/src/main/res/layout/activity_launcher_settings.xml`

Confirmed source facts (source evidence, not screenshot evidence):

1. Grid columns/rows are currently read from `workspace_prefs` SharedPreferences (`key_grid_cols`, `key_grid_rows`; defaults 4x5). `LauncherSettingsActivity` writes those values; the AI grid tool also writes those same preference keys. There is no persisted Room grid configuration entity/row. Grid values are therefore shared by those call sites, but not stored as part of the Room layout records.
2. `MainActivity.refreshGridAndSettings()` updates only `workspace.getActiveCellLayout()`. It is called from `onResume()`. `WorkspacePagerAdapter.onCreateViewHolder()` separately hard-codes every new `CellLayout` to 4 columns x 5 rows. Thus the roadmap's inconsistent-grid issue still exists for inactive/new pages and pages rebound later.
3. `CellLayout.countX/countY` setters reset the occupancy matrix. Existing child views are not removed/re-registered when dimensions change. `addViewToCell()` may relocate a colliding item visually to a vacant cell without updating the entity; widget insertion passes `preventOverlap = false`. This does not establish a safe persistent reflow for grid changes.
4. `WorkspaceItemEntity` stores Room-backed `cellX`, `cellY`, `spanX`, `spanY`, `screenId`, and `container`. The entity has no database-level bounds/collision checks. The database is Room schema version 3; this task does not authorize schema/migration changes.
5. `WorkspaceViewModel.updateWidgetSpan()` copies spans and updates the Room row without visible bounds/collision validation. Widget picker estimates spans from provider minimum dimensions and clamps them to 1..4 by 1..5, regardless of the current selected grid.
6. Workspace `ViewPager2` and `PageIndicatorView` are implemented. The indicator draws one dot per page when page count exceeds one and tracks ViewPager callbacks; that confirms implementation, not that the supplied Nova screenshots show a similar indicator.
7. `WorkspaceRepository` initializes two pages and starter placements, and provides Room update/insert operations. Its initialization uses fixed 4-column coordinates. It does not provide a grid-change transaction/reflow operation.
8. Dragging an app from the drawer constructs a transient workspace entity with default `id = 0`; on an empty-cell drop, `DragController` routes it through `updateItemPosition()`. The repository looks up the id first and returns when absent, so this path does not insert/persist the drawer app. Existing item drag repositioning uses the Room updater. Folder movement also updates Room through repository methods.
9. `CellLayout.addViewToCell()` returns false if no vacant cell exists, but adapter calls do not inspect the result. Out-of-grid coordinates are clamped for point-to-cell conversion; occupancy marking silently returns for out-of-bounds spans. A widget view may still be added with its stored out-of-bounds layout parameters because its adapter call disables overlap prevention.
10. Current grid settings UI is an AlertDialog titled “Home Screen Grid Dimension”, with fixed 3x4, 4x5 (Default), and 5x6 choices. It writes preferences and updates summary text, but there is no grid preview, custom row/column control, or adapter-wide refresh in the inspected code. The settings layout is a dark custom page, materially different from the supplied Nova settings list screenshots (#12–13).
11. The source provides Room persistence for item positions and SharedPreferences for grid values. Actual cold-restart behavior was not exercised in this task, so runtime survival is not newly confirmed.

## Comparison: screenshot requirement vs LauncherAI

Classifications describe evidence available for Phase 9.1; **UNKNOWN** means the reference screenshot evidence is insufficient. Visual similarity alone is not functional equivalence.

| Requirement/surface | Reference screenshot evidence | LauncherAI source evidence | Classification |
|---|---|---|---|
| Home screen settings entry | Home screen row, subtitle “Set grid, icon layout, dock settings, and more” | One dark settings hub row “Grid Size & Layout” and summary | DIFFERENT (entry presentation and hierarchy) |
| Grid dimensions controls | Not shown | Dialog has 3x4, 4x5, 5x6 choices | UNKNOWN for reference parity; current controls confirmed |
| Authoritative grid persistence | Not exposed visually | SharedPreferences keys are used by settings, MainActivity and AI grid tool; no Room grid record | PARTIAL; runtime consistency and post-restart untested |
| Same grid on every workspace page | Screenshots do not show page grids | Active page updated; adapter pages default to 4x5 | MISSING (source confirms gap) |
| Preserve existing item/folder positions | Not shown | Room stores positions, but grid resizing has no coordinated reflow/validation | UNKNOWN reference; MISSING safe-change behavior in current source |
| Out-of-bounds/collision handling | Not shown | UI may shift collisions only in memory; persistence can diverge; widgets skip overlap prevention | MISSING robust validation/persistence |
| Widget spans | No widget screen/control shown | Stored spans, picker estimate fixed to 4x5 bounds; span updater unvalidated; no resize frame in reviewed source | UNKNOWN reference; PARTIAL current hosting/placement support, with validation gap |
| Immediate all-page UI update | No grid edit interaction shown | Only active attached CellLayout is changed; page adapter dimensions remain fixed | MISSING |
| Persistence across restart | No restart scenario shown | Room stores placements; grid prefs stored in SharedPreferences, no runtime restart test performed | UNKNOWN from runtime evidence |
| ViewPager2/page indicators | Home screenshot does not establish page count or indicator | ViewPager2 plus indicator implementation present | UNKNOWN visual reference; implementation confirmed |
| Grid-settings visual style | Home screen settings category visible, detail absent | Dark settings activity/dialog differs from the light Nova settings home in supplied screenshots | DIFFERENT at settings-shell level; grid-detail parity UNKNOWN |

No screenshot-confirmed Nova grid rows, columns, range, defaults, empty-cell handling, resize policy, collision policy, or grid-dialog appearance can be extracted from this set.

## Phase 9.1 relevance and revised scope

Directly relevant reference evidence is limited to: (a) the settings hierarchy places “Home screen” under Nova Launcher settings and explicitly describes it as containing grid, icon layout, and dock settings; and (b) onboarding and settings share Nova's visible visual language, while the supplied Home screen itself confirms a paged-launcher context only weakly. The grid detail screen is missing, so exact control layout and exact grid options must remain an unresolved reference requirement.

The revised prompt in `docs/PHASE_9_1_REVISED_IMPLEMENTATION_PROMPT.md` scopes implementation to one persisted grid configuration consumed consistently by all workspace pages; preservation and validated deterministic recovery of stored placements; widget span checks; immediate UI updates; restart persistence; and compatibility with existing Room, MVVM, ViewPager2, and drag/drop architecture. It also calls for matching only screenshot-supported Nova settings structure/style, while explicitly requiring review/input for any unseen grid-detail specifics instead of inventing them.

## Completion report

- **Screenshots successfully inspected:** all 13 supplied screenshots, #1–#13 above.
- **Screenshots that could not be accessed:** none.
- **Relevant source files inspected:** the 16 paths listed in “Source inspection and current implementation findings”.
- **Confirmed implementation gaps:** only active page grid refresh; hard-coded 4x5 dimensions for newly created page views; no safe persisted grid-change/reflow; occupancy reset without re-registration; unchecked span updates; widget bounds fixed to 4x5; non-persistent drawer-to-empty-cell placement path; current settings visual shell differs from reference settings screenshots.
- **Revised implementation scope:** see `docs/PHASE_9_1_REVISED_IMPLEMENTATION_PROMPT.md`.
- **Unresolved questions:** the exact Nova Home screen grid settings screenshot/control set is absent; desired grid choices/default/minimum/maximum and exact out-of-bounds recovery policy cannot be verified from provided screenshots. The document prompt asks implementer to avoid inventing visual details and flag these for review if no further reference is supplied.

**Stopped after creating the requested analysis and revised prompt documents. Phase 9.1 implementation has not started.**
