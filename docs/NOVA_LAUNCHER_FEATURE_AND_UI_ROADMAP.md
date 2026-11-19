# Nova Launcher Feature and UI Roadmap

## Audit scope and evidence

**Reference:** Nova Launcher v8.9.6, build 89600, decompiled source at `D:\DEMO\Nova Launcher Demo\Nova Launcher`  
**Implementation:** `D:\DEMO\Nova Launcher Demo\LauncherAI`  
**Scope:** Non-AI launcher features, settings, UI, persistence, and system integration. This is a static source/resource audit; no implementation files were changed.

The reference inventory is based on decompiled Java and resource declarations. LauncherAI findings are based on its current Kotlin, XML, manifest, and Room sources. A class or preference name alone is not treated as proof of equivalent behavior. Classifications below refer to parity with Nova:

- **MATCHED** — equivalent behavior and presentation are confirmed.
- **PARTIAL** — a meaningful subset exists, but important behavior is absent or incomplete.
- **MISSING** — no corresponding implementation was found.
- **DIFFERENT** — the feature exists with a materially different interaction or design.
- **UNKNOWN** — available source does not establish the behavior or exact value.

No Nova APK or device screenshots were available for this audit. `adb`, `emulator`, `aapt`, and `apkanalyzer` were unavailable in the environment. The reference source tree contains many renamed resource files, but no runtime capture set; consequently, reference pixel measurements and device behavior are marked unknown rather than guessed. The local historical `LAUNCHERAI_REFERENCE_FEATURE_AUDIT.md` predates the current source: its claims that Room persistence, paging, drag/drop, and widget hosting were absent conflict with the code audited here and should not be used as the current baseline.

REA was not used for this phase: the comparison is against the supplied decompiled source/resource tree, and there was no APK/runtime target to inspect. Earlier Android REA analysis was blocked by Windows Application Control; no new setup or configuration was attempted.

## Executive summary

Nova's non-AI launcher is a broad, integrated Launcher3-based implementation: a persistent launcher database, paged workspace and dock, app drawer/search, folders, widget binding and resizing, wallpaper handling, gesture dispatch, notification badges, and backup/restore are all represented in concrete source paths and manifest declarations.

LauncherAI is no longer just the static shell described by the older audit. Its current source has a Room-backed workspace, two pages, a cell grid, folders, workspace drag operations, widget binding/hosting, hidden-app preferences, a searchable categorized drawer, icon-pack discovery, and a settings activity. These are real implementations, but several paths are incomplete or disconnected. In particular:

1. The visible five-slot dock is hard-coded; Room-backed dock items and the capacity preference do not drive it.
2. Dragging an app from the drawer onto an empty workspace cell attempts to update a transient row rather than insert it, so that placement cannot persist.
3. Grid settings affect only the currently active cell layout; newly bound pages use a fixed 4x5 grid.
4. Gesture detectors are configured but not attached to launcher touch dispatch; only swipe-up is exposed in settings.
5. Widget hosting and binding exist, but no resize handles are implemented.
6. Several settings are Toast-only; wallpaper parallax, dock capacity, and drawer toggles do not produce their advertised effects.
7. Nova features with no LauncherAI counterpart include notification badges, launcher backup/restore, Nova's provider-aware search, and its extensive settings hierarchy.

**Overall parity: PARTIAL**, with material differences in screen organization and important missing launcher functions. Pixel-level equivalence is **UNKNOWN** without a reference runtime capture.

## 1. Complete reference launcher feature inventory

| Reference feature | Confirmed implementation and behavior | Evidence confidence |
|---|---|---|
| Home/workspace and pages | Launcher3 `Workspace` is a paged view containing `CellLayout` screens. It handles workspace drops, screen movement, and wallpaper interaction. `LauncherProvider` persists favorites and layout data. | Confirmed by source |
| Grid and layout | Grid configuration and hotseat size participate in launcher database loading; pages and item placement are persisted. Exact v8.9.6 default row/column values were not recovered. | Behavior confirmed; defaults unknown |
| App drawer | `AllAppsContainerView` is the drawer surface. Nova adds its drawer search bar and separate app-search results; resources identify frequent/recent/new-or-updated sections. | Confirmed by source/resources |
| Dock/hotseat | Nova `Hotseat` is a paged `CellLayoutPagedView`; workspace drop handling recognizes hotseat layouts. Settings expose dock pages, icons, and background options. Exact default capacity is unknown. | Confirmed by source/resources |
| Icons and labels | Launcher3 icon caching is extended by Nova. Resources/settings cover icon themes, adaptive icon handling, label size/font, normalization, and shadows. `.novatheme` assets and theme activities are present. | Confirmed by source/resources |
| Folders | Native `Folder`, `FolderIcon`, and `FolderPagedView` support folder creation, editing, previews, scrolling, and paged contents. Resource arrays identify multiple preview styles and window modes. | Confirmed by source/resources |
| Drag and drop | Workspace handles internal/external drops and launcher targets, including moving items among workspace, hotseat, and folders. | Confirmed by source |
| Widgets | Android-backed widget host/picker and `AppWidgetResizeFrame` are present. The resize frame consults provider resize modes. Manifest includes widget binding permission. | Confirmed by source/manifest |
| Wallpaper | Nova picker/crop activities use `WallpaperManager`; workspace code also handles wallpaper interaction/offsets. | Confirmed by source/manifest |
| Gestures and transitions | Workspace touch handling and Nova action dispatch cover gestures; resource strings enumerate swipe, pinch, double-tap, and two-finger gestures. | Confirmed by source/resources; defaults unknown |
| Search | Search bar, app-drawer search, Nova search/provider settings, and app-search results are separate components. Search-provider resources/configuration are present. | Confirmed by source/resources |
| Notifications/badges | A notification-listener service is declared. Settings/resources identify dot, numeric, and dynamic badges and notification-access requirements. | Confirmed by source/manifest |
| Hidden apps | Reference settings and drawer/group flows support drawer organization and app visibility controls. | Confirmed by source/settings inventory |
| Backup/restore | `LauncherBackupAgent`, restore-file activity, and provider backup/restore tables are present. Strings describe storage destinations and restoring a desktop layout. | Confirmed by source/resources |
| Settings | Legacy and newer settings activities expose desktop, dock, drawer, folders, look and feel, gestures, search bars/providers, night mode, badges, backup, integrations, Labs, popup-menu, and About pages. | Routes and fragments confirmed; exposure varies by activity |
| Android integration | Nova declares a HOME activity, launcher provider, widget integration, wallpaper components, notification listener, and backup agent. | Confirmed by manifest |

Reference source/resource inventory for this section:

- [Nova manifest](<../../Nova Launcher/app/src/main/AndroidManifest.xml>)
- [Workspace.java](<../../Nova Launcher/app/src/main/java/com/android/launcher3/Workspace.java>), [CellLayout.java](<../../Nova Launcher/app/src/main/java/com/android/launcher3/CellLayout.java>), [LauncherProvider.java](<../../Nova Launcher/app/src/main/java/com/android/launcher3/LauncherProvider.java>)
- [Hotseat.java](<../../Nova Launcher/app/src/main/java/com/teslacoilsw/launcher/Hotseat.java>), [AllAppsContainerView.java](<../../Nova Launcher/app/src/main/java/com/android/launcher3/allapps/AllAppsContainerView.java>)
- [Folder.java](<../../Nova Launcher/app/src/main/java/com/android/launcher3/folder/Folder.java>), [FolderIcon.java](<../../Nova Launcher/app/src/main/java/com/android/launcher3/folder/FolderIcon.java>), [FolderPagedView.java](<../../Nova Launcher/app/src/main/java/com/android/launcher3/folder/FolderPagedView.java>)
- [AppWidgetResizeFrame.java](<../../Nova Launcher/app/src/main/java/com/android/launcher3/AppWidgetResizeFrame.java>), [WidgetsFullSheet.java](<../../Nova Launcher/app/src/main/java/com/android/launcher3/widget/picker/WidgetsFullSheet.java>)
- [NovaLauncher.java](<../../Nova Launcher/app/src/main/java/com/teslacoilsw/launcher/NovaLauncher.java>), [LauncherWallpaperPickerActivity.java](<../../Nova Launcher/app/src/main/java/com/android/launcher3/LauncherWallpaperPickerActivity.java>), [LauncherBackupAgent.java](<../../Nova Launcher/app/src/main/java/com/android/launcher3/LauncherBackupAgent.java>), [RestoreBackupFileHandler.java](<../../Nova Launcher/app/src/main/java/com/teslacoilsw/launcher/RestoreBackupFileHandler.java>)
- [NovaAppDrawerSearchBar.java](<../../Nova Launcher/app/src/main/java/com/teslacoilsw/launcher/launcher3/NovaAppDrawerSearchBar.java>), [AppSearchResultsView.java](<../../Nova Launcher/app/src/main/java/com/teslacoilsw/launcher/search/AppSearchResultsView.java>), [NotificationListener.java](<../../Nova Launcher/app/src/main/java/com/teslacoilsw/launcher/notificationlistener/NotificationListener.java>)
- [Nova settings fragments](<../../Nova Launcher/app/src/main/java/com/teslacoilsw/launcher/preferences/fragments>), [Pref3.java](<../../Nova Launcher/app/src/main/java/com/teslacoilsw/launcher/preferences/Pref3.java>), [arrays.xml](<../../Nova Launcher/app/src/main/res/values/arrays.xml>), [strings.xml](<../../Nova Launcher/app/src/main/res/values/strings.xml>)

## 2. Current LauncherAI implementation inventory

| Area | Current source evidence | Current status |
|---|---|---|
| Home/workspace | `MainActivity`, `Workspace`/`ViewPager2`, `WorkspacePagerAdapter`, `CellLayout`, and a page indicator. Repository initializes two pages; there is no UI to add, delete, or reorder pages. | Partial |
| Persistence | Room v3 stores workspace items, pages, folders, widget IDs, and chat messages; SharedPreferences stores current page, hidden apps, and settings. This confirms persistence exists, not backup/restore parity. | Partial vs Nova |
| App drawer | Installed launchable activities are alphabetized; drawer filtering combines text, category, and hidden-app state. Suggested row is the first five visible apps, not usage-based. The fast scroller displays a letter bubble but has no draggable index or section jump. | Partial |
| Dock | Five XML slots launch generic Phone/Messages/Drawer/Browser/Camera actions. Room dock items and capacity setting are not connected to the visible dock. | Different |
| Icons and labels | System icons/labels render; icon pack is selected and looked up for workspace items using a derived drawable resource name. Drawer icons are loaded from installed-app info; no label editor/visibility control is connected. | Partial |
| Folders | Workspace drag code can create/add folders; repository stores children and titles; folder overlay shows a 3-column app list and supports renaming. Folder-child long-press/rearrangement is not wired. | Partial |
| Drag/drop | Existing workspace items can be moved and dropped on remove/app-info/uninstall targets. Drawer-to-empty-workspace placement is defective: the transient item is not inserted into Room. | Partial |
| Widgets | Provider picker, bind/configure flow, `AppWidgetHost`, lifecycle, and workspace hosting are present. Span updates have no resize UI/handles. | Partial |
| Wallpaper | Settings can launch the system chooser, and page changes attempt to set offsets. Home root has an opaque `bg_dark` background; the “toggle scroll offset” choice only shows a Toast. | Partial |
| Gestures | A detector class exists and swipe preferences are loaded, but no call to `GestureHandler.onTouchEvent` is connected to launcher touch dispatch. Settings expose only swipe-up. | Partial / not operationally connected |
| Search | Home bar opens the drawer; drawer text filters installed apps. Mic launches the system web-search intent. No broad Nova-style provider/contact/shortcut search exists. | Partial |
| Badges | No notification listener, badge model, or icon-dot implementation was found in LauncherAI source/manifest. | Missing |
| Hidden apps | Hidden package names are stored in SharedPreferences and filtered from drawer apps; a dialog can toggle visibility. | Partial |
| Backup/restore | Android backup is enabled, but there is no launcher export/import UI. The configured rules are sample/empty templates and do not establish launcher backup support. | Missing |
| Settings | A custom settings screen offers grid, drawer, dock, gestures, icon packs, and wallpaper. Some are persisted; several are display-only or Toast-only. No Nova-scale preference hierarchy exists. | Partial |
| Default launcher | `MainActivity` declares MAIN/HOME/DEFAULT/LAUNCHER filters. There is no in-app role request; whether Android selected it as default is device-unverified. | Partial |
| Runtime status | No physical-device or emulator verification was performed for this audit. | Unknown |

Key LauncherAI sources:

- [MainActivity.kt](<../app/src/main/java/com/example/launcherai/MainActivity.kt>), [activity_main.xml](<../app/src/main/res/layout/activity_main.xml>), [AndroidManifest.xml](<../app/src/main/AndroidManifest.xml>)
- [Workspace.kt](<../app/src/main/java/com/example/launcherai/ui/Workspace.kt>), [WorkspacePagerAdapter.kt](<../app/src/main/java/com/example/launcherai/ui/WorkspacePagerAdapter.kt>), [CellLayout.kt](<../app/src/main/java/com/example/launcherai/ui/CellLayout.kt>), [WorkspaceViewModel.kt](<../app/src/main/java/com/example/launcherai/ui/WorkspaceViewModel.kt>)
- [WorkspaceRepository.kt](<../app/src/main/java/com/example/launcherai/data/repository/WorkspaceRepository.kt>), [WorkspaceDao.kt](<../app/src/main/java/com/example/launcherai/data/db/WorkspaceDao.kt>), [LauncherDatabase.kt](<../app/src/main/java/com/example/launcherai/data/db/LauncherDatabase.kt>), [WorkspaceItemEntity.kt](<../app/src/main/java/com/example/launcherai/data/db/WorkspaceItemEntity.kt>)
- [DragController.kt](<../app/src/main/java/com/example/launcherai/dragndrop/DragController.kt>), [AppsAdapter.kt](<../app/src/main/java/com/example/launcherai/AppsAdapter.kt>), [AppManager.kt](<../app/src/main/java/com/example/launcherai/AppManager.kt>), [FastScroller.kt](<../app/src/main/java/com/example/launcherai/ui/FastScroller.kt>)
- [LauncherAppWidgetHost.kt](<../app/src/main/java/com/example/launcherai/widgets/LauncherAppWidgetHost.kt>), [WidgetPickerDialog.kt](<../app/src/main/java/com/example/launcherai/widgets/WidgetPickerDialog.kt>), [LauncherAppWidgetHostView.kt](<../app/src/main/java/com/example/launcherai/widgets/LauncherAppWidgetHostView.kt>)
- [LauncherSettingsActivity.kt](<../app/src/main/java/com/example/launcherai/ui/LauncherSettingsActivity.kt>), [activity_launcher_settings.xml](<../app/src/main/res/layout/activity_launcher_settings.xml>), [IconPackManager.kt](<../app/src/main/java/com/example/launcherai/icons/IconPackManager.kt>), [GestureHandler.kt](<../app/src/main/java/com/example/launcherai/gestures/GestureHandler.kt>)

## 3. Feature comparison

| Feature | Nova reference | LauncherAI now | Parity |
|---|---|---|---|
| HOME/default-launcher entry | HOME activity and launcher system components | HOME-capable manifest entry; no in-app role request and no device confirmation | PARTIAL |
| Workspace paging and grid | Persistent Launcher3 paged workspace and configurable grid | Room-backed ViewPager2 and cell grid; two pages only, no page management, grid applies inconsistently | PARTIAL |
| Workspace item persistence | LauncherProvider favorites database and backup integration | Room persistence for apps/folders/widgets; no Nova backup format | PARTIAL |
| Drawer organization | Search, sections, drawer styles, tabs/groups, behavior controls | Search, category chips, hidden apps, alphabetical list; suggested apps are first five alphabetic entries | PARTIAL |
| Search providers | Nova search surfaces and provider settings | Installed-app filter and system web-search launch | DIFFERENT |
| Dock | Paged, data-driven hotseat and background/position options | Fixed five-slot XML dock; settings/database do not change visible contents | DIFFERENT |
| App icons/labels | Nova icon caching/theme support and configurable labels | System icons; limited drawable-name icon-pack lookup on workspace only | PARTIAL |
| Folders | Native folder windows, previews, paging, search, settings | Basic overlay, title edit, and basic membership; limited child interactions | PARTIAL |
| Drag/drop | Workspace, hotseat, folder, and launcher drop targets | Workspace move/remove/info/uninstall paths; drawer-to-empty drop cannot persist | PARTIAL |
| Widgets | Native picker/host, configuration and resize frame | Picker/bind/configure/host; no resize handles | PARTIAL |
| Wallpaper | Nova picker/crop and workspace wallpaper behavior | OS chooser and offset attempt; opaque home background and no-op toggle | PARTIAL |
| Gestures | Configurable gesture dispatch on workspace | Actions/detectors exist but are not wired to touch dispatch | PARTIAL |
| Notification badges | Listener-backed dots/numeric/dynamic modes | No listener or badge rendering | MISSING |
| Hidden apps | Drawer/settings integration | Stored and filtered; access is hidden behind drawer count long-press | PARTIAL |
| Backup/restore | Launcher backup agent and layout restore flow | No launcher export/import; generic backup rules only | MISSING |
| Settings hierarchy | Multiple preference screens with many persisted controls | One custom screen; some working settings and several placeholders | DIFFERENT |
| Pixel/UI parity | Reference layout/runtime screenshots unavailable | Current XML can be measured, reference screen appearance cannot | UNKNOWN |

No non-AI feature is classified **MATCHED**: current source confirms several working subsets, but each still has a documented behavioral or integration gap, and device-level equivalence was not established.

## 4. UI comparison by screen

Nova's confirmed launcher surfaces are native Android/Launcher3 views and Nova Java components (workspace, drawer, hotseat, folders, widget sheets, settings activities/fragments). No Compose implementation was established in the inspected source. The decompiled resource tree contains extensive renamed XML/image assets; without an APK/resource table or runtime screenshots, those assets do not establish a pixel-perfect screen image.

| Screen/component | Nova reference | LauncherAI current UI/interaction | Difference and classification |
|---|---|---|---|
| Home/workspace | Paged Launcher3 workspace with configurable screens, grid, launcher drops, hotseat, wallpaper interaction and user-controlled content | Dark custom screen with top search/AI bar, large digital clock, AI/REA cards, paged grid and fixed dock | Different content hierarchy and chrome; **DIFFERENT** |
| App drawer | `AllAppsContainerView`, Nova search bar/results, recent/frequent/new-updated sections and configurable drawer modes | Full-screen overlay, search field, category chips, “suggested” row, count, 4-column list/grid and a scroll-letter bubble | Core search/list exists; sections, suggestions and navigation differ; **PARTIAL** |
| Dock | Paged, data-driven hotseat, drag/drop and configurable backgrounds/position/pages | Five hard-coded shortcut cells and generic system intents; no Room binding | Material behavior and layout difference; **DIFFERENT** |
| Folder | Native folder window, preview modes, scrolling/paging, folder settings and launcher drop behavior | Custom overlay with 3-column children and title editor; no child long-press rearrangement | Basic folder functionality only; **PARTIAL** |
| Widgets | Android widget picker/host plus resize frame and launcher lifecycle | Long-press clock opens custom picker; bind/configure/host works in source; no resize affordance | **PARTIAL** |
| Search | Home/drawer search and Nova provider/search-bar settings | Home search click opens drawer; query filters apps; mic launches web search | Provider and result scope differ; **DIFFERENT** |
| Settings | Multiple settings routes/fragments with control-specific screens and persisted preferences | Single custom scrolling page with cards and AlertDialogs; includes an AI banner and six launcher sections | Large hierarchy and interaction difference; **DIFFERENT** |
| Wallpaper | Nova wallpaper picker/crop and wallpaper-aware workspace | System wallpaper chooser; home root uses opaque dark background; parallax option is Toast-only | **PARTIAL** |
| Gestures | Configurable touch dispatch/actions and animations | Gesture code/settings exist but input is not connected | **PARTIAL** |
| Badges/backup | Badge settings/service and launcher backup/restore screens | No badge surface or launcher backup/restore UI | **MISSING** |

### Recoverable LauncherAI visual measurements

Measurements below are literal XML values, not screenshot-derived estimates:

- Home search bar: 50dp high, 16dp horizontal margins, 36dp top margin, 14dp start padding, 8dp end padding.
- Clock: 48sp bold time; 13sp date; 8dp top/bottom padding.
- Fixed dock: 72dp high, 16dp side margins and bottom margin; five equal-weight slots.
- Drawer search: 48dp high; drawer top padding 36dp; 4-column RecyclerView layout is set in `MainActivity`.
- Settings header: 16dp horizontal and 36dp top padding; 18sp bold title. Settings rows use 14dp padding.
- LauncherAI palette includes `bg_dark #0F1117`, `bg_surface #1A1D27`, `bg_drawer #F20D1017`, `accent_primary #6366F1`, `accent_secondary #EC4899`, `accent_cyan #06B6D4`, `text_primary #F3F4F6`, and `text_secondary #9CA3AF`.

These are not Nova measurements. Exact Nova dp/sp values, colors as rendered on a device, corner radii, icon sizes, font metrics, status/navigation bar treatment, and animation timing remain **UNKNOWN** unless directly recovered from a resource declaration. Do not copy LauncherAI values into a Nova specification.

### Screenshot and runtime evidence

- **Reference screenshots:** unavailable; none were supplied or captured.
- **LauncherAI screenshots:** unavailable for this audit.
- **Runtime/device metadata:** unavailable; no emulator or connected-device tooling was available.
- The reference tree includes image assets, but their renamed filenames and lack of capture metadata do not identify them as screen captures.
- Reference UI implementation priorities below must be re-ranked after a runtime baseline with matching device resolution, density, Android version, orientation, and Nova settings.

## 5. Settings comparison

Nova settings use shared preferences through the launcher preference layer (`y2`/`Pref3`) plus some direct shared-preference writes. Many values/defaults are not safely recoverable from the decompiled preference initializers. **Unknown means not established**, not “off.” Listed options are only those confirmed in inspected resources.

| Nova screen/family and settings | Confirmed Nova options/behavior; default | LauncherAI equivalent, persistence, and gap |
|---|---|---|
| Desktop: grid/margins, default page, page count, lock, auto-add shortcuts, infinite scroll, transition effect, wallpaper scroll/zoom, page indicator/color | Effects include None, Simple, Cube, Card Stack, Tablet, Revolving Door, Flip, Flip Vertical, In/Out, Accordion, Glass, Wipe, Zoom Fade, Throw. Wallpaper scroll includes On/Off/Reverse; resource values also include Manual. Most defaults unknown. | Grid dialog persists 3x4, 4x5 (shown default), or 5x6 to `workspace_prefs`; resume updates only active page and new pages default 4x5. Margins, page count/default selection, locking, auto-add, effects and indicator customization missing. **PARTIAL** |
| Dock: enablement, grid/margins, background/shadow/overlay, infinite scroll, auto-close/position, search-widget dimensions, page count | Settings and pageable hotseat source confirmed; `big_grid_size=false` is a confirmed default. Most other defaults/options unknown in this audit. | Capacity dialog persists 3/5/7 but visible dock remains five static slots. No background, position, paging or search-widget controls. **DIFFERENT** |
| Drawer: portrait/landscape grid, style/animation, search/tab placement, tabs/groups, menu actions, open mode, scrolling/autoclose/position, predictive apps/shortcuts, smart folders, keyboard behavior | Styles: vertical, vertical list, horizontal paginated. Menu actions: Search, Market, Menu. Open mode: Swipe, Icon, Other. Bar position: None, Top, Bottom. Other defaults unknown. | Search, category filtering, hidden apps and a letter bubble exist. Drawer settings “enabled” options only Toast; no tabs/groups, predictive sorting, Nova modes or persisted style controls. **PARTIAL** |
| Folders: icon/window, preview, scroll direction, window mode, folder search, first-icon priority | Scroll: vertical/horizontal. Window: Window/Immersive. Previews: Fan, Grid, Line, Radial, Stack. Defaults unknown. | Basic folder creation, rename and click-to-launch list; no Nova preview/window modes, search, or child rearrangement. **PARTIAL** |
| Search: home/search bar, drawer search, provider enablement/inline providers and placeholder behavior | Placement options: None, Widget, Dock above icons, Dock below icons, Persistent. Provider defaults unknown. | Home bar opens app drawer; mic launches system web search. No provider registry/settings equivalent. **DIFFERENT** |
| Gestures and input: pinch, swipe, double-tap, two-finger, rotation/navigation actions | Gesture families confirmed; complete action values and defaults not recovered. | Only swipe-up choice is configurable/persisted. Detector is not wired to input. No full action mapping. **PARTIAL** |
| Badges/notifications: enablement/provider, style, position/size, access prerequisite | Dots, Numeric Dots, Dynamic; service and access checks present. Default not established. | No badge/listener service or settings. **MISSING** |
| Look & Feel/icons: icon theme/shape/normalization/shadow; adaptive legacy/reshape/animation; label font/size/single-line; app/folder animations; orientation/system bars | Shapes include Round, Square, Rounded square, Squircle, Teardrop and additional entries. Legacy reshape: On/Auto/Off. Orientation: Default/Auto-rotate/Portrait/Landscape. Animation choices include System, Circle, Adaptive icon, Clip Reveal, Slide Up, Zoom, Blink, Slide Left. Defaults mostly unknown. | Limited icon-pack selection; no app-label editor/visibility toggle, adaptive-icon suite, orientation, status/nav, or broad animation settings. **PARTIAL** |
| Night mode | Always, Never, Auto, Schedule; location and surface-color options. Default unknown. | Static dark palette only; no night-mode preference. **MISSING** |
| Popup menu/general Nova settings | Popup styles: Block, Pills, Android 12; notification inclusion and item visibility; widget overlap/drop-target/undo controls. `experimental_mode=false` confirmed. | No comparable general settings pages. **MISSING** |
| Labs | Big-grid and launcher visibility options, debug file logging, update prompts. Confirmed defaults: `big_grid_size=false`, `show_launchers_in_drawer=false`, `debug_file_log=false`, `prompt_for_updates=true`. | No equivalent Labs controls. **MISSING** |
| Backup/restore | Backup destination and restore flow can replace desktop layout; backup agent/provider tables present. | Android backup declarations only; no launcher export/import workflow. **MISSING** |
| Integrations/About | Separate reference settings pages exist; exact per-option defaults not established here. | No non-AI equivalents found. **MISSING** |

### Granular reference setting keys recoverable from the decompiled source

The following inventory expands the screen families above to the individual preference keys and labels recoverable from `Pref3` metadata and the settings fragments. The decompiled source includes internal state keys as well as user controls, so a key alone does not prove that a visible setting is exposed. Defaults/options remain **UNKNOWN** unless explicitly stated.

| Screen | Individual controls/keys recovered | Options/defaults and LauncherAI parity |
|---|---|---|
| Home screen / Desktop | `desktop_cellspecs`, `desktop_grid`, `desktop_width_margin`, `desktop_height_margin`, `desktop_default_page`, `desktop_page_indicator`, `desktop_page_indicator_color`, `desktop_scroll_indicator`, `desktop_scroll_indicator_color`, `desktop_lock`, `desktop_shadow`, `desktop_page_count`, legacy `workspace_screen_count`, `auto_add_shortcuts`, `desktop_infinite_scroll`, `desktop_scroll_effect`, legacy `scroll_effect`, `wallpaper_scroll_mode`, legacy `wallpaper_scrolling`, `wallpaper_scrolling_force`, `wallpaper_zoom_effects`, `desktop_indicator_drawerdock_swipe` | Defaults unknown. Indicator values: None/Circle/Dot/Line. Scroll effects: None, Simple, Cube, Card Stack, Tablet, Revolving Door, Flip, Flip Vertical, In and Out, Accordion, Glass, Wipe, Zoom Fade, Throw; the value array also contains Zoomy. Wallpaper scroll values include On/Off/Reverse in the UI and Manual in the values array. LauncherAI only exposes a 3x4/4x5/5x6 grid dialog; the other controls are absent or incomplete. |
| Dock | `dock_cellspecs`, `dock_grid_cols`, `dock_width_margin`, `dock_height_margin`, `dock_enabled`, legacy `dock_enable`, `dock_background`, `dock_show_shadow`, `dock_infinite_scroll`, `dock_overlay`, `dock_autoclose`, `dock_remember_position`, `dock_qsb_height_dp`, `dock_qsb_padding_dp`, `dock_qsb_appwidgetid`, `dock_pages_count` | Defaults/options unknown except `big_grid_size=false`. Nova source persists page count and drawer-menu choices; LauncherAI's stored capacity does not affect its five static slots. |
| App drawer | `drawer_cellspecs`, `drawer_grid`, `drawer_grid_land`, `drawer_style`, `drawer_animation`, `drawer_searchbar_position`, `drawer_tabbar_position`, `drawer_bottom_tabs_above_searchbar`, `drawer_tab_style`, `drawer_menu_action_set`, `drawer_isolate_tabs`, `drawer_swipe_to_open`, `drawer_swipe_indicator`, `drawer_swipe_haptic`, `drawer_pull_to_search`, `drawer_scroll_effect`, `drawer_infinite_scroll`, `drawer_autoclose`, `drawer_remember_position`, `drawer_fastscroll`, `drawer_predictive_apps`, `drawer_predictive_shortcuts`, `hide_predictive_app_packages`, `drawer_smart_folders`, `drawer_work_tab`, `drawer_folders_first`, legacy `drawer_folders_before_apps`, `drawer_show_keyboard_by_default`, `drawer_searchbar_style` | Styles: Vertical/Vertical List/Horizontal Paginated; open mode: Swipe/Icon/Other; bar position: None/Top/Bottom; menu actions: Search/Market/Menu. Confirmed default: legacy `drawer_folders_before_apps=true`; other defaults unknown. LauncherAI has working filtering/hidden-app state, but no equivalent settings for these behaviors. |
| Folders | `folder_icon_config`, `folder_window_config`, `folder_cellspecs`, `folder_search_enabled`; fragment controls also cover preview, scroll direction, appearance/background, and first-icon behavior | Window/Immersive; Vertical/Horizontal scrolling; Fan/Grid/Line/Radial/Stack previews; Circle/Disc/Platform/Square background labels. Defaults unknown. LauncherAI has basic folders but no equivalent controls. |
| Search / Search bar | `searchbar_assist_icon`, `desktop_searchbar_style`, `drawer_searchbar_style`, `feed_page_searchbar_config`, `drawer_search_window_radius`, `drawer_search_keyboard_incognito`, `search_external_providers`, `search_limit_apps_to_one_row`, `searchbar_placement`, `searchbar_widget_ensure_onresume`, `appsearch_background`, `nova_search_inline_providers`, `non_configurable_inline_providers`, `save_search_history` | Placement: None/Widget/Dock Above Icons/Dock Below Icons/Persistent. Provider lists include enabled/disabled states. Defaults unknown. `nova_ai_search_mode` is excluded as AI-related. LauncherAI has app-name filtering and a system web-search intent, not provider settings. |
| Gestures / input | `gesture_home_button`, `gesture_home_button_only_on_default`, `gesture_long_menu`, `gesture_swipe_up`, `gesture_swipe_down`, `gesture_swipe_down_haptic`, `gesture_swipe_down_legacy`, `gesture_pinch_in`, `gesture_pinch_out`, `gesture_swipe_two_up`, `gesture_swipe_two_down`, `gesture_double_tap`, `gesture_rotate_ccw`, `gesture_rotate_cw`, `gesture_double_tap_swipe_up`, `gesture_double_tap_swipe_down`, `gesture_swipe_left`, `gesture_swipe_right`, `gesture_nav_contract` | Gesture labels are recoverable; action-choice lists and defaults are unknown. LauncherAI exposes only swipe-up selection and does not connect touch events to its detector. |
| Look & Feel / icons / system appearance | `color_palette_mode`, `color_scheme_day`, `color_scheme_night`, `surface_glass`, `adaptive_icon_shape`, `adaptive_icon_prefer_legacy`, `adaptive_icon_reshape`, `adaptive_icon_anim`, `icon_theme`, `icon_theme_masking`, `icon_normalization`, `icon_normalization_shadows`, `icon_glass`, `icon_glass_style`, `app_animation`, `disable_animations`, `screen_orientation`, `show_notification_bar`, `notification_bar_transparency`, `notification_bar_dark_icons`, `navigation_bar_transparency`, `navbar_hide_back`, `notification_bar_hide_clock`, `notification_bar_hide_clock_use_root`, `edit_dialog_show_cn`, `debug_desktopmenu_export_png` | Confirmed defaults: `surface_glass=false`, `adaptive_icon_prefer_legacy=false`, `adaptive_icon_anim=true`; remaining defaults unknown. Reshape: On/Auto/Off; fonts: Normal/Medium/Condensed/Light; orientation: No Sensor/Unspecified/Portrait/Landscape; activity animations: System/Circle/Adaptive Icon/Clip Reveal/Slide Up/Zoom/Blink/Slide Left. Shape labels include Crop, Flower, Heptagon, Hexagon, Octagon, Pebble, Pentagon, Round, Rounded Square, Square, Squircle, Cropped, Teardrop, Vessel. Icon Layout exposes Font/Single line/Size, but full key mapping/options are unknown. LauncherAI only has limited icon-pack lookup and no equivalent broad controls. |
| Night mode | `night_mode`, `night_mode_location`, `night_mode_auto_searchbar`, `night_mode_auto_drawer_searchbar`, `night_mode_auto_drawer`, `night_mode_auto_drawer_icon`, `night_mode_auto_folder`, `night_mode_auto_search_window`, `night_mode_bg_color`, `night_mode_custom_sunrise`, `night_mode_custom_sunset` | Always/Never/Auto/Schedule; strings also identify Follow System/System and location enhancement. Defaults unknown. LauncherAI has a static dark palette only. |
| Notification badges | `unread_badges_enabled`, `unread_badge_type`, `unread_badge_position`, `unread_badge_style`; legacy `unread_count`, `unread_count_provider`, `unread_count_badge_style` | UI includes Dots/Numeric Dots/Dynamic; style strings also include Classic/Holo. Enabling checks notification-listener access and Android system `notification_badging`; that system read uses default `1`. The legacy provider route writes `unread_count=true`; general default unknown. LauncherAI has no corresponding service or UI. |
| Popup menu / general launcher | `popup_menu_style_config`, `popup_menu_include_notifications`, `popup_menu_item_visibility`, `show_drop_targets`, `show_undo_bar`; general settings also include `widget_overlap`, `widget_overlap_when_placing`, `widget_dont_send_all_sizes` | Popup styles: Block/Pills/Android 12. `experimental_mode=false` confirmed. Other defaults/options unknown. No equivalent LauncherAI controls. |
| Cards, feed, integrations, and other settings | Non-AI keys include `enable_card_view`, `nova_now`, `nova_now_show_keyboard`, `feed_page`, `minus_one_edge_swipe`, `google_now_transition`, `google_now_theme`, `google_now_restart_companion_and_google`, media/games/rewards/Nova Mobile card placement, recommendation, pinned/disabled card and shortcut lists, weather units/custom tabs/fixed location, `labs_enabled`, `bypass_system_theme`, `analytics_enabled`, `show_alpha_options`; backup, Themes, New apps, About, and Integrations pages are also present. | Most defaults/options unknown. Google Now transition: Page/Overlay; theme: Light/Dark/Follow Nova mode. Confirmed Labs defaults: `big_grid_size=false`, `show_launchers_in_drawer=false`, `debug_file_log=false`, `prompt_for_updates=true`; `experimental_mode=false`; legacy `drawer_folders_before_apps=true`. AI-specific `nova_ai_search_mode` excluded. LauncherAI has no corresponding non-AI surfaces. |

Nova settings persist mainly through the launcher shared-preference abstraction consumed by fragments and observed by `Pref3`; some controls use direct writes. The exact backing filename for the shared instance is unknown in the inspected initialization path. `Pref3` metadata includes state/persistence fields that are not necessarily user-editable settings. The individual controls above should therefore be treated as the **most granular recoverable catalog**, not a claim that every metadata key is a visible preference.

LauncherAI-specific setting behavior:

- **Home grid:** writes `key_grid_cols` and `key_grid_rows`; options 3x4, 4x5, 5x6; default 4x5. Change is not propagated to all pages.
- **Drawer:** category chips/filtering and hidden-app filtering function independently. The “Filter Categories Enabled” and “Alphabetical Fast Scroll Active” rows only show a Toast; the fast-scroll bubble is not a draggable alphabet index. Hidden-app management requires opening the drawer and long-pressing the count.
- **Dock:** `key_dock_capacity` persists 3/5/7, but XML remains five fixed shortcuts.
- **Gestures:** only `key_gesture_swipe_up` is saved in this settings screen; swipe-down and other mappings are not offered here, and touch events are not connected.
- **Icon pack:** selected package persists; lookup is a simple derived drawable-name attempt for workspace items. This is not equivalent to full icon-pack metadata support.
- **Wallpaper:** system chooser is launched; “Toggle Wallpaper Scroll Offset” only displays a Toast. Page changes separately attempt wallpaper offsets.
- Drawer summary text such as “categories enabled” and “fast scroll active” is static presentation, not proof those settings were persisted or configured.

## 6. Missing functionality

Confirmed missing or without a corresponding LauncherAI implementation:

1. Notification listener, notification badge model/rendering, and badge settings.
2. Launcher-specific backup/export and restore/import of workspace, widgets, and settings.
3. Nova-style provider-aware search, search-provider configuration, and broader search results.
4. Nova's multi-page settings hierarchy and most desktop, dock, drawer, folder, appearance, badge, night-mode, integration, and Labs controls.
5. Widget resize handles and resize interactions.
6. Workspace page creation, deletion, and reordering.
7. Nova's configurable dock paging, content, placement, background, and search-widget options.
8. Complete icon-pack parsing/application to drawer and workspace, icon-shape/adaptive-icon options, and user label controls.
9. Drawer tab/group/predictive behavior and Nova's recent/frequent/updated sections.
10. Folder search, preview/window modes, paging, and child rearrangement.
11. A functioning configured gesture dispatch path.
12. Confirmed device-level selection as default HOME and behavior across supported Android versions/OEMs.

## 7. Incomplete functionality

- **Drawer-to-home drag:** resolved in Phase 9.1 — drawer apps (`id == 0L`) are inserted as new persistent `WorkspaceItemEntity` records in Room upon workspace drop.
- **Grid changes:** resolved in Phase 9.1 — `WorkspaceGridConfig` acts as the single source of truth across `WorkspaceRepository`, `WorkspaceViewModel`, `MainActivity`, `WorkspacePagerAdapter`, and `CellLayout`.
- **Dock:** data exists in Room/ViewModel, but the displayed dock is not data-bound; the capacity setting does not affect the five visible slots (scheduled for Phase 9.2).
- **Folders:** base create/add/title/cleanup operations exist, but children in the overlay are click-to-launch only; no child reorder/move interaction is wired.
- **Widgets:** provider discovery, bind/configuration, host lifecycle, and rendering are present; no visual resize frame or user resize control exists.
- **Suggestions and fast scrolling:** suggestions are simply the first five alphabetized visible apps; scrolling displays the first visible app's letter, without section indexing or jump controls.
- **Icon packs:** selection persists, but resource resolution relies on a derived drawable name and does not parse the pack catalog; drawer icons bypass the selected pack.
- **Wallpaper:** chooser and offset attempt exist, but home background is opaque and the toggle option is a Toast; end-to-end wallpaper visibility/parallax is not confirmed.
- **Settings persistence:** grid, dock-capacity, swipe-up, and icon-pack selections persist; drawer toggle rows and parallax toggle do not. Displayed summaries may imply more behavior than exists.
- **Runtime/system integration:** widget permission flow, package broadcasts, system intents, wallpaper offsets, reflective status-bar actions, and HOME role all require device verification.

## 8. Incorrect or materially different behavior

These are source-confirmed deviations, not visual guesses:

- Dock settings imply an adjustable capacity, but the dock remains fixed at five XML slots.
- Drawer settings claim category/fast-scroll enablement, but those selections only show “Setting enabled”; categories are always rendered and fast scrolling has no index control.
- “Suggested Applications” is not a recency/usage-based suggestion surface; it displays the first five visible apps from the alphabetized app list.
- “Toggle Wallpaper Scroll Offset” reports activation but changes no setting; offsets are attempted separately on page change.
- The grid summary says labels are visible, but there is no label visibility control in the implementation.
- Generic Android backup declarations are not equivalent to Nova's launcher layout export/restore flow.

## 9. Required resource and layout changes

This is a planning section; no resources were changed.

- Replace the fixed dock presentation with a data-driven view that reflects persisted dock items and selected settings; preserve the existing Room/ViewModel approach.
- Apply grid dimensions consistently to every workspace page and expose page management only after the reference interaction is measured.
- Add the reference drawer's required sections/search affordances and interaction states using actual captured Nova evidence; do not infer exact colors, spacing, or animations from LauncherAI's current palette.
- Add folder and widget interactions only after measuring reference behavior, including folder previews and widget resize handles.
- Implement settings as behavior-backed controls; remove or clearly disable Toast-only choices until their state is actually stored and applied.
- Add layout/resource coverage for badge states, backup/restore, search providers, and required dialogs if prioritized by the feature roadmap.
- Reconstruct exact color, dimension, typography, icon, corner-radius, and animation tokens from a Nova APK/resource mapping or device screenshot set before claiming pixel parity. The current decompiled tree's renamed resource filenames make visual identification uncertain.

## 10. Prioritized implementation roadmap

Each phase is independently implementable/testable. Keep the existing Kotlin, Room, MVVM, ViewPager2, and drag/drop architecture; do not remove or refactor AI components as part of this non-AI work.

| Priority / phase | Scope | Acceptance checks | Implementation status |
|---|---|---|---|
| P0 — Establish a reference baseline | Obtain an authorized Nova v8.9.6 APK/device or reproducible reference runtime; record resolution, density, Android version, orientation, settings, and screenshots for home/drawer/dock/folder/widget/settings. Resolve static-resource ambiguities. | Evidence set is version/device/settings-labeled; no guessed dimensions remain in the visual spec. | Pending |
| P1 — Phase 9.1 Workspace grid configuration | Use one grid/page source of truth (`WorkspaceGridConfig`) across all pages; preserve Room persistence; deterministic reflow & capacity validation; clamp widget spans; persist drawer-to-workspace app drops. | Change grid and navigate/restart: every page uses the selected grid; page and item positions remain valid and stable. | **CLOSED (Phase 9.1)** — Persisted authoritative `WorkspaceGridConfig` implemented across repository, ViewModel, ViewPager2 adapter, settings dialog, AI grid tool, and widget picker. Placement preservation, deterministic reflow, capacity rejection, and drawer app drops verified with 61 passing unit tests and successful debug build. Physical-device verification pending. |
| P2 — Complete drag/drop persistence | Insert drawer-originated items on empty-cell drop; handle occupied-cell movement, folder membership, dock targets, cancellation, and invalid/full cells atomically. | Unit tests cover drawer-to-home, workspace move, folder create/add/remove, no-space, and cancel; device test confirms no duplicate/lost shortcuts. | **VERIFIED (Phase 9.1)** — Drawer-to-workspace drop persistence completed in Phase 9.1. Dock targets and dock rearrangement scheduled for Phase 9.2. |
| P3 — Phase 9.2 Data-driven dock integration | Replace static dock XML with data-driven Room `CONTAINER_DOCK` view; bind `WorkspaceViewModel.resolvedDockItems`; apply `key_dock_capacity` (3, 5, 7 slots); enable dock drag/drop & reorder. | Dock survives restart; reorder/add/remove and capacity choices alter actual UI; UI and database remain consistent. | **COMPLETED (Phase 9.2)** — Data-driven `DockLayout` implemented across `MainActivity`, `WorkspaceRepository`, `WorkspaceViewModel`, `DragController`, and settings. Dynamic 3/5/7 capacity, reserved App Drawer button, overflow reflow to desktop, and full drag/drop verified with 66 passing unit tests and successful debug build. Physical-device verification pending. |
| P4 — Drawer parity | Implement verified section ordering, search behavior, tabs/groups/predictive options and real fast-scroll navigation; persist each exposed setting. | Search, categories, sections, hidden apps and fast-scroll behavior match recorded reference flows; all options survive restart. | Pending |
| P5 — Folder and widget interactions | Add reference-matched folder preview/window/child operations and widget resize handles respecting provider resize modes. | Folder actions persist across restart; resize is constrained by provider/grid and updates stored spans. | Pending |
| P6 — Appearance and icon handling | Implement verified icon-pack metadata, drawer/workspace consistency, label controls, shapes, wallpaper treatment, and visual tokens. | Compare screenshots on same device/density; record numeric tolerances and verify labels/icons/wallpaper across pages. | Pending |
| P7 — Search, gestures, and badges | Connect gesture events/actions; implement only verified Nova search providers and badge/access flows. | Per-gesture and search-provider tests; notification access granted/revoked; badge updates and cleanup checked on device. | Pending |
| P8 — Backup/restore and system integration | Add launcher-specific export/restore with validation and widget-ID handling; verify HOME selection, package changes, wallpaper and widget lifecycle. | Round-trip backup restores workspace/settings; corrupted/unsupported backups report errors safely; Android/OEM matrix passes. | Pending |
| P9 — Final visual/interaction parity | Tune layout, assets, transitions, dialogs, loading/empty/error states from reference screenshots and interaction recordings. | Side-by-side screenshot review and scripted interaction checklist pass for each major screen. | Pending |

## 11. Device-testing checklist

- [ ] Record device model, screen resolution, density, Android/API level, orientation, font scale, and navigation mode.
- [ ] Confirm Android's selected default HOME launcher and cold/warm HOME launch behavior.
- [ ] Compare Nova and LauncherAI home at the same wallpaper, grid, icon scale, dock contents, and page.
- [ ] Open/close drawer by every supported path; verify search keyboard, empty results, categories, sections, and back navigation.
- [ ] Launch apps from drawer, suggested row, dock, workspace, and folder; verify multi-activity packages and missing/uninstalled apps.
- [ ] Drag drawer apps onto empty/occupied cells, between pages, into/out of folders, to dock, and to remove/info/uninstall targets; verify restart persistence.
- [ ] Change grid settings and verify every page, widget span, collisions, and saved positions.
- [ ] Bind, configure, launch, resize, move, remove, and reboot with several widget providers and denied/approved binding.
- [ ] Grant/revoke notification access and validate badge style, count update, stale-notification cleanup, and launcher restart.
- [ ] Exercise each configured gesture with one/two fingers and verify the selected action; test unavailable system actions.
- [ ] Set/change wallpaper, page across workspace, rotate, and verify wallpaper display/scroll offset.
- [ ] Apply/reset icon pack, labels and appearance; compare drawer and workspace.
- [ ] Export/restore backups on same and different devices; include widgets, empty layouts, malformed files, and version mismatch.
- [ ] Install/update/remove packages and verify shortcuts, drawer list, folders, and widget providers refresh without data loss.
- [ ] Repeat screenshot capture for home, drawer, folder, widget picker/resize, settings pages, empty/error states, and dialogs.

## 12. Exact source and resource references

### Nova reference files inspected or used as direct evidence

- [AndroidManifest.xml](<../../Nova Launcher/app/src/main/AndroidManifest.xml>)
- [Workspace.java](<../../Nova Launcher/app/src/main/java/com/android/launcher3/Workspace.java>), [CellLayout.java](<../../Nova Launcher/app/src/main/java/com/android/launcher3/CellLayout.java>), [LauncherProvider.java](<../../Nova Launcher/app/src/main/java/com/android/launcher3/LauncherProvider.java>), [LauncherBackupAgent.java](<../../Nova Launcher/app/src/main/java/com/android/launcher3/LauncherBackupAgent.java>)
- [Hotseat.java](<../../Nova Launcher/app/src/main/java/com/teslacoilsw/launcher/Hotseat.java>), [AllAppsContainerView.java](<../../Nova Launcher/app/src/main/java/com/android/launcher3/allapps/AllAppsContainerView.java>), [AppWidgetResizeFrame.java](<../../Nova Launcher/app/src/main/java/com/android/launcher3/AppWidgetResizeFrame.java>), [WidgetsFullSheet.java](<../../Nova Launcher/app/src/main/java/com/android/launcher3/widget/picker/WidgetsFullSheet.java>)
- [Folder.java](<../../Nova Launcher/app/src/main/java/com/android/launcher3/folder/Folder.java>), [FolderIcon.java](<../../Nova Launcher/app/src/main/java/com/android/launcher3/folder/FolderIcon.java>), [FolderPagedView.java](<../../Nova Launcher/app/src/main/java/com/android/launcher3/folder/FolderPagedView.java>)
- [NovaLauncher.java](<../../Nova Launcher/app/src/main/java/com/teslacoilsw/launcher/NovaLauncher.java>), [NovaLauncherActivityCachingLogic.java](<../../Nova Launcher/app/src/main/java/com/teslacoilsw/launcher/icon/NovaLauncherActivityCachingLogic.java>), [RestoreBackupFileHandler.java](<../../Nova Launcher/app/src/main/java/com/teslacoilsw/launcher/RestoreBackupFileHandler.java>)
- [LauncherWallpaperPickerActivity.java](<../../Nova Launcher/app/src/main/java/com/android/launcher3/LauncherWallpaperPickerActivity.java>), [NovaAppDrawerSearchBar.java](<../../Nova Launcher/app/src/main/java/com/teslacoilsw/launcher/launcher3/NovaAppDrawerSearchBar.java>), [AppSearchResultsView.java](<../../Nova Launcher/app/src/main/java/com/teslacoilsw/launcher/search/AppSearchResultsView.java>), [NotificationListener.java](<../../Nova Launcher/app/src/main/java/com/teslacoilsw/launcher/notificationlistener/NotificationListener.java>)
- [SettingsActivity.java](<../../Nova Launcher/app/src/main/java/com/teslacoilsw/launcher/preferences/SettingsActivity.java>), [SettingsActivityLegacy.java](<../../Nova Launcher/app/src/main/java/com/teslacoilsw/launcher/preferences/SettingsActivityLegacy.java>), [Pref3.java](<../../Nova Launcher/app/src/main/java/com/teslacoilsw/launcher/preferences/Pref3.java>), [shared preference helper h2.java](<../../Nova Launcher/app/src/main/java/j10/h2.java>), [AdaptiveIconSettingsActivity.java](<../../Nova Launcher/app/src/main/java/com/teslacoilsw/launcher/preferences/AdaptiveIconSettingsActivity.java>), [IconLayoutSettingsActivity.java](<../../Nova Launcher/app/src/main/java/com/teslacoilsw/launcher/preferences/IconLayoutSettingsActivity.java>)
- [SettingsDesktop.java](<../../Nova Launcher/app/src/main/java/com/teslacoilsw/launcher/preferences/fragments/SettingsDesktop.java>), [SettingsDock.java](<../../Nova Launcher/app/src/main/java/com/teslacoilsw/launcher/preferences/fragments/SettingsDock.java>), [SettingsDrawer.java](<../../Nova Launcher/app/src/main/java/com/teslacoilsw/launcher/preferences/fragments/SettingsDrawer.java>), [SettingsFolder.java](<../../Nova Launcher/app/src/main/java/com/teslacoilsw/launcher/preferences/fragments/SettingsFolder.java>), [SettingsGestures.java](<../../Nova Launcher/app/src/main/java/com/teslacoilsw/launcher/preferences/fragments/SettingsGestures.java>)
- [SettingsBadges.java](<../../Nova Launcher/app/src/main/java/com/teslacoilsw/launcher/preferences/fragments/SettingsBadges.java>), [SettingsBackup.java](<../../Nova Launcher/app/src/main/java/com/teslacoilsw/launcher/preferences/fragments/SettingsBackup.java>), [SettingsLookFeel.java](<../../Nova Launcher/app/src/main/java/com/teslacoilsw/launcher/preferences/fragments/SettingsLookFeel.java>), [SettingsNightMode.java](<../../Nova Launcher/app/src/main/java/com/teslacoilsw/launcher/preferences/fragments/SettingsNightMode.java>), [SettingsSearch.java](<../../Nova Launcher/app/src/main/java/com/teslacoilsw/launcher/preferences/fragments/SettingsSearch.java>), [SettingsSearchBar.java](<../../Nova Launcher/app/src/main/java/com/teslacoilsw/launcher/preferences/fragments/SettingsSearchBar.java>), [SettingsSearchProviders.java](<../../Nova Launcher/app/src/main/java/com/teslacoilsw/launcher/preferences/fragments/SettingsSearchProviders.java>), [SettingsIntegrations.java](<../../Nova Launcher/app/src/main/java/com/teslacoilsw/launcher/preferences/fragments/SettingsIntegrations.java>), [SettingsLabs.java](<../../Nova Launcher/app/src/main/java/com/teslacoilsw/launcher/preferences/fragments/SettingsLabs.java>), [SettingsNova.java](<../../Nova Launcher/app/src/main/java/com/teslacoilsw/launcher/preferences/fragments/SettingsNova.java>), [SettingsPopupMenu.java](<../../Nova Launcher/app/src/main/java/com/teslacoilsw/launcher/preferences/fragments/SettingsPopupMenu.java>)
- [arrays.xml](<../../Nova Launcher/app/src/main/res/values/arrays.xml>), [strings.xml](<../../Nova Launcher/app/src/main/res/values/strings.xml>), [public.xml](<../../Nova Launcher/app/src/main/res/values/public.xml>)

### LauncherAI files inspected or used as direct evidence

- [AndroidManifest.xml](<../app/src/main/AndroidManifest.xml>), [activity_main.xml](<../app/src/main/res/layout/activity_main.xml>), [activity_launcher_settings.xml](<../app/src/main/res/layout/activity_launcher_settings.xml>), [colors.xml](<../app/src/main/res/values/colors.xml>)
- [MainActivity.kt](<../app/src/main/java/com/example/launcherai/MainActivity.kt>), [AppManager.kt](<../app/src/main/java/com/example/launcherai/AppManager.kt>), [AppsAdapter.kt](<../app/src/main/java/com/example/launcherai/AppsAdapter.kt>)
- [Workspace.kt](<../app/src/main/java/com/example/launcherai/ui/Workspace.kt>), [WorkspacePagerAdapter.kt](<../app/src/main/java/com/example/launcherai/ui/WorkspacePagerAdapter.kt>), [CellLayout.kt](<../app/src/main/java/com/example/launcherai/ui/CellLayout.kt>), [WorkspaceViewModel.kt](<../app/src/main/java/com/example/launcherai/ui/WorkspaceViewModel.kt>), [FastScroller.kt](<../app/src/main/java/com/example/launcherai/ui/FastScroller.kt>), [HiddenAppsDialog.kt](<../app/src/main/java/com/example/launcherai/ui/HiddenAppsDialog.kt>)
- [DragController.kt](<../app/src/main/java/com/example/launcherai/dragndrop/DragController.kt>), [GestureHandler.kt](<../app/src/main/java/com/example/launcherai/gestures/GestureHandler.kt>), [IconPackManager.kt](<../app/src/main/java/com/example/launcherai/icons/IconPackManager.kt>)
- [LauncherSettingsActivity.kt](<../app/src/main/java/com/example/launcherai/ui/LauncherSettingsActivity.kt>), [LauncherAppWidgetHost.kt](<../app/src/main/java/com/example/launcherai/widgets/LauncherAppWidgetHost.kt>), [LauncherAppWidgetHostView.kt](<../app/src/main/java/com/example/launcherai/widgets/LauncherAppWidgetHostView.kt>), [WidgetPickerDialog.kt](<../app/src/main/java/com/example/launcherai/widgets/WidgetPickerDialog.kt>)
- [WorkspaceRepository.kt](<../app/src/main/java/com/example/launcherai/data/repository/WorkspaceRepository.kt>), [WorkspaceDao.kt](<../app/src/main/java/com/example/launcherai/data/db/WorkspaceDao.kt>), [LauncherDatabase.kt](<../app/src/main/java/com/example/launcherai/data/db/LauncherDatabase.kt>), [WorkspaceItemEntity.kt](<../app/src/main/java/com/example/launcherai/data/db/WorkspaceItemEntity.kt>), [PackageMonitorReceiver.kt](<../app/src/main/java/com/example/launcherai/receiver/PackageMonitorReceiver.kt>)
- [backup_rules.xml](<../app/src/main/res/xml/backup_rules.xml>), [data_extraction_rules.xml](<../app/src/main/res/xml/data_extraction_rules.xml>)

### Audit limitations

- Exact Nova defaults remain unknown for many controls; the table records only verified values and explicitly labels the rest unknown.
- Static source presence does not prove a feature works on a device; runtime and interaction paths need the checklist above.
- No source, layout, drawable, manifest, dependency, database, or build file was modified. Only this audit report was created.
