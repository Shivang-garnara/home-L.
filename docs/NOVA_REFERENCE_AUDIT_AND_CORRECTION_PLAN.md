# Nova Launcher Reference UI & Flow Parity Gap Audit and Correction Plan

## 1. Executive Summary & Authoritative Evidence Context

This document provides a genuine, rigorous, line-by-line screenshot-by-screenshot and flow-by-flow comparison between the authoritative Nova Launcher 8.9.6 reference screenshots (located in `D:\DEMO\Nova Launcher Demo\Photos`) and the current state of LauncherAI.

### Authoritative Reference Source
- **Location:** `D:\DEMO\Nova Launcher Demo\Photos` (13 original reference screenshots, ending in timestamps `10-17-59` through `10-19-20`).
- **Prior Analysis Document:** `docs/PHASE_9_1_REFERENCE_SCREENSHOT_ANALYSIS.md`.
- **Reference Device Profile:** 720 x 1600 px @ 320 dpi (density factor = 2.0; 2 px = 1 dp; **360 x 800 dp** viewport).

---

## 2. Authoritative Reference Inventory (13 Reference Screenshots)

| # | Filename / Timestamp | Screen Identity | Authoritative Visual & Flow Evidence |
|---|---|---|---|
| **01** | `01 10-17-59` | **Welcome** | Dark wallpaper gradient (`#151316` top, `#160805` mid, `#211D1C` bot), Nova logo mark, "Welcome to Nova Launcher", orange pill button "Get started" (`#D54526`, 118x40dp, 20dp radius), bottom "Restore backup" & "Skip setup" links. |
| **02** | `02 10-18-07` | **Default Chooser (Setup)** | Native Android `RoleManager` default home app chooser dialog over dimmed setup wallpaper (`#FCEAE4` pale peach card, 28dp radius). |
| **03** | `03 10-18-17` | **Plan / Paywall** | Subscription paywall overlay (EXCLUDED from LauncherAI per non-commercial free app instructions). |
| **04** | `04 10-18-21` | **Setup: Search** | Dark setup frame, back arrow, 25% orange progress bar, "Search" title, Mode dropdown ("Immersive"), Shortcut Search toggle (ON), Contact Search toggle (OFF), Home search bar placement dropdown ("Dock (Below icons)"), App drawer placement dropdown ("Top"), orange next FAB (55x55dp, 18dp radius). |
| **05** | `05 10-18-38` | **Setup: App drawer** | Dark setup frame, 50% orange progress bar, "App drawer" title, "Vertical" style dropdown, toggles ON for Show Keyboard, Frequently used apps, Frequently used shortcuts, Smart Folders, orange next FAB. |
| **06** | `06 10-18-42` | **Setup: Icon style** | Dark setup frame, 75% orange progress bar, "Icon style" title, 4 color swatches, "Icon size 125%" value + minus/plus/slider, shape choices (Rounded square, Round, Squircle, Flower, "More" button), orange next FAB. |
| **07** | `07 10-18-45` | **More shapes dialog** | Pale lavender card modal (`#EDE5F4`, 312x418dp, 28dp radius) titled "More shapes", 4-column grid (Square, Teardrop, Pebble, Vessel, Pentagon, Hexagon, Hexagon 2nd variant, Heptagon, Octagon, Crop). No mid-word label wrap. |
| **08** | `08 10-18-51` | **All done!** | Dark setup frame, orange checked circle header, "All done!" title, centered sparkle icon, "Setup is finished", full-width orange pill button "Go home" (`#D54526`, 328x48dp, 24dp radius). |
| **09** | `09 10-18-55` | **Default Chooser (Home)** | Native Android `RoleManager` chooser dialog over live home screen wallpaper (triggered when exiting setup if launcher is not default). |
| **10** | `10 10-19-01` | **Home Screen** | Full system wallpaper background (`#00000000`), Nova Settings shortcut top-right, Gmail & Play Store mid-lower shortcuts, 5-icon dock directly on wallpaper (NO text labels, NO glass panel), pale peach pill search bar (`#FEDACE`, 340x55dp, 28dp radius) BELOW dock with 9-dot grid left & mic right. |
| **11** | `11 10-19-08` | **App Drawer** | Light solid background `#FFFBFF`, top pale peach search pill ("Search apps", `#FEDBCF`, 328x44dp, 22dp radius) with 9-dot grid icon, "Nova Settings" rounded suggestion chip, "No Frequent Apps" empty state + thin divider line, 5-column grid starting with Smart Folders (2x2 preview tiles e.g. Entertainment, Games) then A-Z apps, right scrollbar thumb (`#7D5245`). |
| **12** | `12 10-19-16` | **Settings Main** | Light warm surface `#FFF8F6`, title "Nova Launcher" (32sp geometric sans-serif `#2D1611`), pale pink search pill field "Search" (`#F5E0DA`, 328x48dp, 24dp radius), red-outlined warning banner ("Not set as default", 2dp red border `#BF0100`, `#FFF8F6` fill), ordered rows: Nova AI, Home screen, App drawer, Folders; bottom nav bar (Settings [selected pill indicator `#F4EFF3`] and Style tabs). |
| **13** | `13 10-19-20` | **Settings Scrolled** | Light warm surface `#FFF8F6`, top search pill, all 12 rows in order (Search, Cards, Look & feel, Gestures & inputs, Feed page, Integrations, Notification badges with blue `#4883B7` "Prime" badge, Backup & restore), bottom nav bar. |

---

## 3. Genuine Screenshot-by-Screenshot & Flow-by-Flow Gap Analysis

### Flow A: First-Run Onboarding Flow (`Ref 01` $\rightarrow$ `02` $\rightarrow$ `04` $\rightarrow$ `05` $\rightarrow$ `06` $\rightarrow$ `07` $\rightarrow$ `08` $\rightarrow$ `10` $\rightarrow$ `09`)

| Step / Ref | Reference Evidence | Current LauncherAI Implementation | Gap / Discrepancy | Required Correction |
|---|---|---|---|---|
| **01 Welcome** | Dark wallpaper gradient, logo mark, "Welcome to Nova Launcher", orange pill button "Get started" (`#D54526`), "Restore backup", "Skip setup". | `SetupActivity` implemented, but on fresh app launch, `key_first_run_completed` persisted as `true` on disk from earlier runs, causing `MainActivity` to skip `SetupActivity` completely. | **FLOW GAP:** Startup routing skipped `SetupActivity` on app start if SharedPreferences previously saved `true`. | Enforce `checkFirstRunWizard()` in `MainActivity` `onCreate()`, `onResume()`, and `onNewIntent()`. Provide `resetSetup()` in `SetupFlowUtils` and Settings. |
| **02 Default Chooser (Setup)** | System `RoleManager` chooser over setup background. | Triggered via `RoleManager.createRequestRoleIntent(ROLE_HOME)` when tapping "Get started". | **FLOW GAP:** On MIUI/Xiaomi devices, `RoleManager` intent requires explicit fallback to `Settings.ACTION_HOME_SETTINGS` if blocked by system permissions. | Implement multi-stage fallback (`ROLE_HOME` $\rightarrow$ `ACTION_HOME_SETTINGS` $\rightarrow$ `ACTION_SETTINGS`). |
| **04 Setup: Search** | Back arrow, 25% orange progress bar, "Search" title, Mode dropdown ("Immersive"), Shortcut Search toggle (ON), Contact Search toggle (OFF), Home placement dropdown ("Dock (Below icons)"), Drawer placement dropdown ("Top"), orange next FAB. | `SetupActivity` step 2 implemented. Preferences saved to `workspace_prefs`. | **VISUAL GAP:** Title uses system `sans-serif-light` font instead of Nova geometric font; subtitle uses "LauncherAI Search" branding. | Document intentional font/branding choices. Ensure Home search placement (`key_search_bar_placement_home`) moves search pill dynamically. |
| **05 Setup: App drawer** | 50% orange progress bar, "App drawer" title, "Vertical" style dropdown, Show Keyboard (ON), Frequently used apps (ON), Frequently used shortcuts (ON), Smart Folders (ON), orange next FAB. | `SetupActivity` step 3 implemented. Preferences saved to `workspace_prefs`. | **APPLICATION GAP:** Preferences are saved, but full application of drawer style, show keyboard, and smart folders requires complete C3 integration. | Apply saved drawer preferences (`key_drawer_show_keyboard`, `key_search_bar_placement_drawer`, `key_drawer_frequent_apps`) in App Drawer overlay. |
| **06 Setup: Icon style** | 75% orange progress bar, "Icon style" title, 4 color swatches, "Icon size 125%" value + slider, shape choices (Rounded square, Round, Squircle, Flower, "More"), orange next FAB. | `SetupActivity` step 4 implemented. Preferences saved to `workspace_prefs`. | **APPLICATION GAP:** Icon size scale % (`key_icon_size_percent`) and shape mask (`key_icon_shape`) are saved, but path masking is not yet applied to workspace icons or dock icons. | Apply `IconShapeMasker.createMaskedIcon` to workspace icons (`Workspace.kt`/`CellLayout.kt`), dock icons (`DockLayout.kt`), and drawer icons (`AppsAdapter.kt`). |
| **07 More shapes dialog** | Pale lavender card modal (`#EDE5F4`, 312x418dp, 28dp radius) titled "More shapes", 4-column grid (Square, Teardrop, Pebble, Vessel, Pentagon, Hexagon, Hexagon 2nd variant, Heptagon, Octagon, Crop). | `MoreShapesDialogFragment` implemented with 10 entries in 4-column grid. | **VISUAL GAP:** Shape previews render flat gray swatches until C5 shape path rendering is active across all icons. | Apply `IconShapeMasker` path masks for all 10 shape variants. |
| **08 All done!** | Orange checked circle header, "All done!" title, centered sparkle icon, "Setup is finished", full-width orange pill button "Go home" (`#D54526`, 328x48dp, 24dp radius). | `SetupActivity` step 5 implemented. "Go home" marks `key_first_run_completed` = `true` and finishes. | **FLOW GAP:** Exiting setup should trigger default launcher check ONCE and open Home screen cleanly without looping. | Ensure `completeSetupAndGoHome()` checks default status ONCE and launches `MainActivity` with `skip_wizard = true`. |

---

### Screen 10: Home Screen (Ref 10)

| Aspect | Reference Evidence (`Screenshot 10 10-19-01`) | Current LauncherAI Implementation | Gap / Discrepancy | Required Correction |
|---|---|---|---|---|
| **Background** | System wallpaper fills screen (`#00000000`). | `rootLauncher` background set to `#00000000`. | **MATCHED.** | None. |
| **Clutter Blocks** | No digital clock block, no AI cards on home. | Digital clock block (`widgetClock`), "Nova AI Chat" (`cardAiBanner`), and "REA Inspector" (`cardEndpoints`) removed from `activity_main.xml`. | **MATCHED.** | None. |
| **Dock Panel** | 5-icon dock directly on wallpaper (NO text labels, NO glass panel). | Glass panel (`bg_dock_glass`) removed from `DockLayout.kt`. Dock height set to 72dp. NO text labels on dock icons. | **PARTIALLY MATCHED.** Slot width is 64dp vs reference ~72dp. | Adjust dock slot width measurement in `DockLayout.kt` to match 72dp slot width. |
| **Bottom Search Bar** | Pale peach search pill (`#FEDACE`, 340x55dp, 28dp radius) BELOW dock icons. 9-dot grid icon left, Assistant dots right. | Search pill (`#FEDACE`, 340x55dp, 28dp radius) placed BELOW dock. Left icon: `ic_apps_drawer` (opens App Drawer). Right icon: `ic_mic` (voice search). Long-press opens AI assistant. | **PARTIALLY MATCHED.** Right icon uses neutral mic VectorDrawable (`ic_mic`) instead of Google 4-color dots. | Document neutral mic icon as an intentional deviation. |
| **Workspace Icons** | Icons directly on wallpaper with 12sp white labels & drop shadow. | Icons directly on wallpaper with white text labels & text shadow. | **PARTIALLY MATCHED.** Font family uses system `sans-serif`. | Apply `IconShapeMasker` to workspace icons. |

---

### Screen 11: App Drawer (Ref 11)

| Aspect | Reference Evidence (`Screenshot 11 10-19-08`) | Current LauncherAI Implementation | Gap / Discrepancy | Required Correction |
|---|---|---|---|---|
| **Background** | Light solid background `#FFFBFF`. | `layoutAppDrawerOverlay` background set to `#FFFBFF`. | **MATCHED.** | None. |
| **Top Search Bar** | Pale peach search pill (`#FEDBCF`, 328x44dp, 22dp radius) with 9-dot grid icon left and "Search apps" hint text (`#53433F`). | Search pill (`#FEDBCF`, 328x44dp, 22dp radius) implemented. Positioned at Top or Bottom via `key_search_bar_placement_drawer`. | **MATCHED.** | Auto-focus search field and open soft keyboard when `key_drawer_show_keyboard` is true. |
| **Suggestion Chips** | Single row of rounded pill chips ("Nova Settings" chip style, background `#FEDBCF`, 16dp radius). | Suggestion chip row implemented ("LauncherAI Settings" chip, background `#FEDBCF`). | **PARTIALLY MATCHED.** Suggestion chip text uses "LauncherAI Settings" branding. | Document branding choice. |
| **Frequent Apps** | Centered empty state text "No Frequent Apps" (`#53433F`) + thin divider line `#D8C5C0`. | Empty state text "No Frequent Apps" + horizontal divider line implemented. Toggled via `key_drawer_frequent_apps`. | **MATCHED.** | None. |
| **App Grid** | 5-column grid layout (`GridLayoutManager`, 5 columns). Smart Folders 2x2 preview tiles displayed first. | `rvDrawerApps.layoutManager = GridLayoutManager(this, 5)` (5 columns). Single-line labels (`#231A18`). | **PARTIALLY MATCHED.** Category smart folders 2x2 preview tiles require dynamic grouping generator. | Generate 2x2 category preview tiles (Entertainment, Games) when `key_drawer_smart_folders` is ON. |
| **Removed Elements**| Category filter chips, "SUGGESTED APPLICATIONS" header, app count text, close chevron button absent. | Category chips, suggested header, app count, and close chevron removed from drawer overlay. | **MATCHED.** | None. |

---

### Screens 12 & 13: Settings Main & Scrolled (Ref 12, 13)

| Aspect | Reference Evidence (`Screenshots 12 & 13`) | Current LauncherAI Implementation | Gap / Discrepancy | Required Correction |
|---|---|---|---|---|
| **Surface** | Light warm surface `#FFF8F6`. | `LauncherSettingsActivity` background set to `#FFF8F6`. | **MATCHED.** | None. |
| **Title** | Large title "Nova Launcher" (32sp geometric sans-serif `#2D1611`). | Title "LauncherAI" (32sp `sans-serif-light`). | **PARTIALLY MATCHED.** Uses system `sans-serif-light` font; title text displays "LauncherAI". | Document intentional font and branding choices. |
| **Search Field** | Top search pill field "Search" (`#F5E0DA`, 328x48dp, 24dp radius). | Top search pill field (`#F5E0DA`, 48dp height, 24dp radius). Real-time search filtering implemented via `SettingsFilterUtils`. | **MATCHED.** | None. |
| **Warning Banner** | Red-outlined warning banner ("Not set as default", 2dp red border `#BF0100`, `#FFF8F6` fill). | Banner implemented with 2dp red border `#BF0100`, `#FFF8F6` fill, 16dp radius. Tapping triggers system `ROLE_HOME` dialog. | **MATCHED.** | None. |
| **Rows Order & Content** | 12 ordered rows: Nova AI, Home screen, App drawer, Folders, Search, Cards, Look & feel, Gestures & inputs, Feed page, Integrations, Notification badges, Backup & restore. | 12 ordered rows with exact subtitles ("AI assistant", "Home screen", "App drawer", "Folders", "Search", "Cards", "Look & feel", "Gestures & inputs", "Feed page", "Integrations", "Notification badges", "Backup & restore"). | **PARTIALLY MATCHED.** Row minHeight is 72dp with 16dp start padding. Subtitles use "LauncherAI" branding. | Document branding choice. |
| **Bottom Nav Bar**| Dual tab bar: Settings (selected, pill indicator `#F4EFF3`) and Style tabs. | Bottom nav bar implemented. Tapping Style tab switches to Style container view; tapping Settings switches back. | **MATCHED.** | None. |

---

## 4. Comprehensive Correction Plan (Phase-by-Phase Execution Order)

### Phase 1: Onboarding Flow & Startup Routing Hardening
1. Enforce `SetupFlowUtils.shouldLaunchSetup()` check in `MainActivity` `onCreate()`, `onNewIntent()`, and `onResume()` so setup opens reliably whenever `key_first_run_completed == false`.
2. Ensure `SetupActivity` Step 1 ("Get started") calls multi-stage fallback for default launcher prompt (`ROLE_HOME` $\rightarrow$ `ACTION_HOME_SETTINGS` $\rightarrow$ `ACTION_SETTINGS`).
3. Ensure Step 5 ("Go home") sets `key_first_run_completed = true`, performs a single non-looping default launcher check (Ref 09), and transitions to Home.

### Phase 2: Icon Sizing & Path Masking Engine Application (C5 Integration)
1. Apply `IconShapeMasker.createMaskedIcon` across all 3 icon surfaces:
   - **Workspace Icons:** `Workspace.kt` / `CellLayout.kt` item binding.
   - **Dock Icons:** `DockLayout.kt` item binding.
   - **App Drawer Items:** `AppsAdapter.kt` item binding.
2. Render exact vector path masks for all 10 shape options: Rounded square, Round, Squircle, Flower, Square, Teardrop, Pebble, Vessel, Pentagon, Hexagon, Heptagon, Octagon, Crop.

### Phase 3: Home Screen & Dock Fine-Tuning (C2 Refinement)
1. Adjust `DockLayout.kt` slot width to 72dp to match reference 10 dock spacing.
2. Confirm Home search pill (`#FEDACE`, 340x55dp, 28dp radius) placement dynamically responds to `key_search_bar_placement_home` (`"DOCK_BELOW"`, `"TOP"`, `"HIDDEN"`).
3. Ensure mic tap opens voice search (`RecognizerIntent.ACTION_RECOGNIZE_SPEECH`) and long-press opens AI assistant (`NovaAiChatActivity`).

### Phase 4: App Drawer Smart Folders & Search Placement (C3 Refinement)
1. Generate dynamic category 2x2 preview tiles ("Entertainment", "Games") as first items in drawer grid when `key_drawer_smart_folders == true`.
2. Apply `key_search_bar_placement_drawer` ("Top" / "Bottom") to position search pill dynamically in `layoutAppDrawerOverlay`.
3. Auto-focus search field and open IME keyboard when `key_drawer_show_keyboard == true`.

---

## 5. Explicit Execution Stop Notice

**IMPLEMENTATION IS STOPPED.** This document completes the authoritative reference inventory, line-by-line gap analysis, and correction plan based strictly on the 13 reference screenshots in `D:\DEMO\Nova Launcher Demo\Photos`. Phase 9.3 execution will not commence until this correction plan is reviewed and approved.
