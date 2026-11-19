# Nova Reference UI & Flow Gap Audit (Phase 9.3R)

## Executive Summary
This document records the exact visual, structural, and behavioral gaps between Nova Launcher 8.9.6 reference screenshots (located in `D:\DEMO\Nova Launcher Demo\Photos`) and the current state of LauncherAI before Phase 9.3R modifications.

---

## Design Tokens (Extracted Directly From Screenshots via Pixel Analysis)

All measurements are sampled directly from 720x1600 px screenshots at 320 dpi (density factor = 2.0; 2 px = 1 dp).

### 1. Color Palette

| Token Name | Hex Code | RGB | Component / Context |
|---|---|---|---|
| **`nova_setup_bg_top`** | `#151316` | (21, 19, 22) | Setup dark wallpaper gradient (top) |
| **`nova_setup_bg_mid`** | `#160805` | (22, 8, 5) | Setup dark wallpaper gradient (middle hero) |
| **`nova_setup_bg_bottom`** | `#211D1C` | (33, 29, 28) | Setup dark wallpaper gradient (bottom) |
| **`nova_accent_orange`** | `#D54526` | (213, 69, 38) | Primary action buttons, FABs, active toggles, setup progress bar, slider track |
| **`nova_orange_outline`** | `#D34527` | (211, 69, 39) | Outlined dropdown border (2dp) |
| **`nova_light_surface`** | `#FFF8F6` | (255, 248, 246) | Settings Activity background, App Drawer background |
| **`nova_search_pill_home`** | `#FEDACE` | (254, 218, 206) | Home screen bottom search bar fill (warm peach tint) |
| **`nova_search_pill_drawer`** | `#FEDBCF` | (254, 219, 207) | App drawer top search pill fill |
| **`nova_search_pill_settings`** | `#F5E0DA` | (245, 224, 218) | Settings top search pill fill |
| **`nova_text_brown_title`** | `#2D1611` | (45, 22, 17) | Settings main title ("Nova Launcher") |
| **`nova_text_brown_primary`** | `#231A18` | (35, 26, 24) | Row item titles, section titles |
| **`nova_text_brown_secondary`**| `#53433F` | (83, 67, 63) | Row item subtitles, descriptive copy |
| **`nova_icon_tint_brown`** | `#7D5245` | (125, 82, 69) | Settings row icon tint, scrollbar thumb tint |
| **`nova_banner_border_red`** | `#BF0100` | (191, 1, 0) | Red warning banner outline border (2dp) |
| **`nova_banner_fill`** | `#FFF8F6` | (255, 248, 246) | Red warning banner background fill |
| **`nova_prime_badge_fill`** | `#4883B7` | (72, 131, 183) | Prime feature badge background fill |
| **`nova_prime_badge_text`** | `#FFFFFF` | (255, 255, 255) | Prime feature badge text |
| **`nova_dialog_surface_lavender`** | `#EDE5F4` | (237, 229, 244) | "More shapes" modal dialog background |
| **`nova_dialog_surface_peach`** | `#FCEAE4` | (252, 234, 228) | System default-home chooser dialog background |

### 2. Dimensional & Shape Tokens

| Component | Width (px / dp) | Height (px / dp) | Corner Radius (dp) | Notes |
|---|---|---|---|---|
| **Setup Orange Button** | 236 px / 118 dp | 80 px / 40 dp | 20 dp | Pill shape, white bold text |
| **Setup Next FAB** | 110 px / 55 dp | 110 px / 55 dp | 18 dp | Rounded square, white right arrow |
| **Outlined Dropdown** | 258 px / 129 dp | 94 px / 47 dp | 20 dp | 2dp orange border, white text + arrow |
| **M3 Switch Toggle** | 102 px / 51 dp | 62 px / 31 dp | 16 dp | M3 spec switch |
| **Home Search Pill** | 680 px / 340 dp | 110 px / 55 dp | 28 dp | 16dp margins, grid left / mic right |
| **Drawer Search Pill** | 656 px / 328 dp | 88 px / 44 dp | 22 dp | 16dp margins |
| **Settings Search Pill** | 656 px / 328 dp | 96 px / 48 dp | 24 dp | 16dp margins |
| **Warning Banner** | 646 px / 323 dp | 149 px / 74 dp | 16 dp | 2dp red border, warning icon left |
| **More Shapes Dialog** | 624 px / 312 dp | 836 px / 418 dp | 28 dp | Pale lavender card |
| **Prime Badge** | 126 px / 63 dp | 46 px / 23 dp | 12 dp | Blue pill badge |

### 3. Typography & Font Tokens

| Style Role | Font Family / Weight | Size (sp) | Color Hex |
|---|---|---|---|
| **Large Heading** | OFL Geometric Sans (`Outfit Light` / `sans-serif-light`) | 28 - 32 sp | `#FFFFFF` (Setup) / `#2D1611` (Light) |
| **Section Header** | Geometric Sans-Serif (Medium) | 14 - 16 sp | `#D54526` (Orange) |
| **Row Item Title** | Sans-Serif (Medium) | 16 sp | `#231A18` |
| **Row Item Subtitle**| Sans-Serif (Regular) | 13 sp | `#53433F` |
| **Button Text** | Sans-Serif (Bold) | 14 - 16 sp | `#FFFFFF` |

**Font Specification Note:** Large titles use an OFL-licensed (SIL Open Font License v1.1) geometric font (`Outfit Light` via Google Fonts / `sans-serif-light` fallback). It is **not** Nova's font and makes no claim to be identical to Nova's custom geometric font.

---

## Intentional Deviations (Not Defects)

1. **LauncherAI Branding:** Large screen titles and subtitles display "LauncherAI" (e.g. "Welcome to LauncherAI", "LauncherAI Search", "Connect other Apps to LauncherAI") per user branding selection.
2. **Prime Badge Omission:** Ref 13 includes a blue `#4883B7` "Prime" badge on Notification Badges. LauncherAI omits all Prime badges and purchase UI because LauncherAI is a free, non-commercial app without subscriptions.
3. **System Bar Icons:** System status bar / navigation bar icons and carrier text reflect the physical test device system bar overlay.

---

## Grid Configuration Options Clarification

The "6x7" grid option is implemented in production code inside `showHomeScreenGridDialog()` in `LauncherSettingsActivity.kt`. Options offered are: `3x4`, `4x5` (Default), `5x6`, and `6x7`. Selected dimensions are persisted to `workspace_prefs` SharedPreferences (`key_grid_cols`, `key_grid_rows`) and applied to `WorkspaceViewModel`.

---

## Detailed Per-Screen Audit Table

| # | Reference Screen | LauncherAI Current File | Visual Differences | Interaction Differences | Source Evidence | Required Correction | Status | Before Screenshot |
|---|---|---|---|---|---|---|---|---|
| 01 | **Welcome** (`01 10-17-59`) | `SetupActivity.kt`, `activity_setup.xml` | **LauncherAI After:** Dark gradient background (`#151316`–`#211D1C`), LauncherAI logo mark, subtitle "Welcome to", title "LauncherAI" (32sp `sans-serif-light`), orange pill button "Get started" (`#D54526`, 118x40dp, 20dp radius), bottom "Restore backup" & "Skip setup" links. | **PARTIALLY MATCHED:** Tapping "Get started" triggers system default launcher prompt (`ROLE_HOME`), then advances to Step 2. Tapping "Skip setup" sets first-run flag and finishes to Home. **Remaining Differences:** Title branding is "LauncherAI" (intentional deviation); font is system `sans-serif-light`. | `SetupActivity.kt`, `activity_setup.xml` | Implemented in Group C4. | **PARTIALLY MATCHED** | `docs/screenshots/launcherai_before/NN_setup.png` |
| 02 | **Default Chooser (Setup)** (`02 10-18-07`) | `SetupActivity.kt` | Native Android system `RoleManager` dialog over setup background. | **MATCHED:** Triggered on "Get started" tap during setup via `RoleManager.createRequestRoleIntent(ROLE_HOME)`. | `SetupActivity.kt` | Implemented in Group C6. | **MATCHED** | N/A (System dialog) |
| 03 | **Plan / Paywall** (`03 10-18-17`) | *None* | Paywall screen exists in Nova reference screenshots. | LauncherAI is an open-source non-commercial launcher with no subscriptions/paywalls. | Explicit project instructions ("The payment/plan page is NOT implemented"). | EXCLUDED per prompt instructions. | **EXCLUDED** | N/A (Excluded) |
| 04 | **Setup: Search** (`04 10-18-21`) | `SetupActivity.kt`, `activity_setup.xml` | **LauncherAI After:** Dark setup frame, back arrow, 25% orange progress bar, title "Search", Mode dropdown ("Immersive"), Shortcut Search toggle (ON), Contact Search toggle (OFF; requests `READ_CONTACTS` when turned ON), Home search bar placement dropdown ("Dock (Below icons)"), Drawer placement dropdown ("Top"), orange next FAB (55x55dp). | **PARTIALLY MATCHED:** Choices saved immediately to SharedPreferences. Home placement (`key_search_bar_placement_home`) APPLIED NOW on Home screen. **Remaining Differences:** System font `sans-serif-light` for title; "LauncherAI Search" branding in subtitle. | `SetupActivity.kt` | Implemented in Group C4. | **PARTIALLY MATCHED** | `docs/screenshots/launcherai_before/NN_setup.png` |
| 05 | **Setup: App drawer** (`05 10-18-38`) | `SetupActivity.kt`, `activity_setup.xml` | **LauncherAI After:** Dark setup frame, back arrow, 50% orange progress bar, title "App drawer", "Vertical" style dropdown, toggles ON for Show Keyboard, Frequently used apps, Frequently used shortcuts, Smart Folders with exact copy, orange next FAB. | **PARTIALLY MATCHED:** All choices saved immediately to SharedPreferences (`key_app_drawer_style`, `key_drawer_show_keyboard`, etc.). **Remaining Differences:** Title font system `sans-serif-light`. | `SetupActivity.kt` | Implemented in Group C4. | **PARTIALLY MATCHED** | `docs/screenshots/launcherai_before/NN_setup.png` |
| 06 | **Setup: Icon style** (`06 10-18-42`) | `SetupActivity.kt`, `activity_setup.xml` | **LauncherAI After:** Dark setup frame, 75% orange progress bar, title "Icon style", 4 color swatches, "Icon size 125%" value + minus/plus/slider, shape choices Rounded square (Default), Round, Squircle, Flower, "More" shape button, orange next FAB. | **PARTIALLY MATCHED:** Choices saved immediately (`key_icon_shape`, `key_icon_size_percent`). Tapping "More" opens lavender dialog (Ref 07). **Remaining Differences:** Title font system `sans-serif-light`. | `SetupActivity.kt` | Implemented in Group C4 & C5. | **PARTIALLY MATCHED** | `docs/screenshots/launcherai_before/NN_setup.png` |
| 07 | **More shapes dialog** (`07 10-18-45`) | `MoreShapesDialogFragment.kt`, `dialog_more_shapes.xml` | **LauncherAI After:** Pale lavender modal card (`#EDE5F4`, 312x418dp, 28dp radius) titled "More shapes", 4-column grid (Square, Teardrop, Pebble, Vessel, Pentagon, Hexagon, Hexagon 2nd variant, Heptagon, Octagon, Crop). | **MATCHED:** Selecting shape updates `key_icon_shape` immediately and dismisses dialog. Labels do not wrap mid-word. | `MoreShapesDialogFragment.kt` | Implemented in Group C4 & C5. | **MATCHED** | `docs/screenshots/launcherai_before/NN_setup.png` |
| 08 | **Setup: All done!** (`08 10-18-51`) | `SetupActivity.kt`, `activity_setup.xml` | **LauncherAI After:** Dark setup frame, orange checked circle icon in header, title "All done!", large centered sparkle icon, "Setup is finished", full-width orange pill button "Go home" (`#D54526`, 328x48dp, 24dp radius). | **PARTIALLY MATCHED:** Tapping "Go home" sets `key_first_run_completed` = `true`, checks default launcher status (triggers ref 09 if not default), and finishes to Home screen. **Remaining Differences:** Title font system `sans-serif-light`. | `SetupActivity.kt` | Implemented in Group C4. | **PARTIALLY MATCHED** | `docs/screenshots/launcherai_before/NN_setup.png` |
| 09 | **Default Chooser (Home)** (`09 10-18-55`) | `SetupActivity.kt`, `LauncherSettingsActivity.kt` | Native Android system `RoleManager` default home chooser over live home screen wallpaper. | **MATCHED:** Triggered when exiting setup if launcher is still not default, or when tapping warning banner in Settings. | `SetupActivity.kt` | Implemented in Group C6. | **MATCHED** | N/A (System dialog) |
| 10 | **Home Screen** (`10 10-19-01`) | `activity_main.xml`, `MainActivity.kt` | **LauncherAI After:** Transparent wallpaper background, digital clock block & AI cards removed, 5-icon dock directly on wallpaper without glass panel, NO text labels on dock icons, pale peach search bar (`#FEDACE`, 340x55dp, 28dp radius) BELOW dock with 9-dot grid icon left & mic icon right. Workspace icons have white text labels. Paging, drag-drop, folders, widgets preserved. | **PARTIALLY MATCHED:** Layout structure, dock placement, bottom search bar position, dock labels, and transparent wallpaper match reference. **Remaining Visual Differences:** (1) System wallpaper varies per device; (2) Search pill right icon uses neutral mic VectorDrawable (`ic_mic`) instead of Google 4-color dots asset; (3) System status bar icons depend on device theme overlay. | `activity_main.xml`, `DockLayout.kt`, `Workspace.kt` | Implemented in Group C2. | **PARTIALLY MATCHED** | `docs/screenshots/launcherai_before/10_Home.png` |
| 11 | **App Drawer** (`11 10-19-08`) | `MainActivity.kt`, `AppsAdapter.kt` | **LauncherAI After:** Light `#FFFBFF` background, top pale peach search pill ("Search apps", `#FEDBCF`, 328x44dp, 22dp radius) with 9-dot grid icon, "LauncherAI Settings" rounded suggestion chip, "No Frequent Apps" empty state + thin divider line, 5-column grid, right scrollbar thumb (`#7D5245`). Category chips, app count, and close chevron removed. | **PARTIALLY MATCHED:** Background color, top search pill, suggestion chip, 5-column grid layout, and empty state match reference. **Remaining Visual Differences:** (1) Suggestion chip text displays "LauncherAI Settings" (intentional branding); (2) App icons rendered with `IconShapeMasker` based on active `key_icon_shape` preference. | `MainActivity.kt`, `AppsAdapter.kt` | Implemented in Group C3. | **PARTIALLY MATCHED** | `docs/screenshots/launcherai_before/11_App_drawer.png` |
| 12 | **Settings Main** (`12 10-19-16`) | `activity_launcher_settings.xml`, `LauncherSettingsActivity.kt` | **LauncherAI After:** Light surface `#FFF8F6`, title "LauncherAI" (32sp `sans-serif-medium`), pale pink search pill field "Search" (`#F5E0DA`, 48dp), red-outlined warning banner ("Not set as default", 2dp red border `#BF0100`, `#FFF8F6` fill), ordered rows: AI assistant (subtitle "Chat and privacy"), Home screen, App drawer, Folders; bottom nav bar (Settings [selected pill indicator `#F4EFF3`] and Style tabs). | **PARTIALLY MATCHED:** Layout structure, color tokens, banner, search filter, and row order match reference. **Remaining Visual Differences:** (1) Font used is Android system default `sans-serif-medium` (not Nova's custom geometric "Nova Sans" font); (2) Title text is "LauncherAI" per user branding choice; (3) Icons are Android Material VectorDrawables; (4) Row minHeight is 64dp with 20dp start margin. | `activity_launcher_settings.xml`, `LauncherSettingsActivity.kt` | Implemented in Step B / Group C1. ViewBinding enabled. | **PARTIALLY MATCHED** | `docs/screenshots/launcherai_before/12_Settings_main.png` |
| 13 | **Settings Scrolled** (`13 10-19-20`) | `LauncherSettingsActivity.kt` | **LauncherAI After:** Light surface `#FFF8F6`, search pill top, all 12 rows in order (Search, Cards, Look & feel, Gestures & inputs, Feed page, Integrations, Notification badges, Backup & restore), bottom nav bar. Subtitles updated to LauncherAI branding ("LauncherAI search window...", "LauncherAI Search...", "Connect other Apps to LauncherAI..."). NO Prime badge rendered per prompt instructions. | **PARTIALLY MATCHED:** Row order, subtitles, search filtering, and "Not available yet" sub-shells match reference. **Remaining Visual Differences:** (1) System font `sans-serif-medium` used instead of Nova geometric font; (2) Prime badge omitted per instructions; (3) VectorDrawables used for row icons. | `activity_launcher_settings.xml`, `LauncherSettingsActivity.kt` | Implemented in Step B / Group C1. ViewBinding enabled. | **PARTIALLY MATCHED** | `docs/screenshots/launcherai_before/13_Settings_scrolled.png` |

---

## Existing LauncherAI Features/Screens With NO Reference Screenshot

The following existing features and screens in LauncherAI have no corresponding screenshot in the 13 reference files provided. Per parity rules, their existing behavior and functionality are strictly preserved, restyled only using Nova shared components where safe, and marked **UNKNOWN**:

| Surface / Screen | LauncherAI Source File | Current Behavior & Status | Policy |
|---|---|---|---|
| **Style Tab Content** | `LauncherSettingsActivity.kt` | No reference screenshot for the "Style" tab in Settings bottom nav bar. | Marked **UNKNOWN**. Render minimal placeholder shell in Style tab and preserve settings options. |
| **Home Screen Grid Detail** | `LauncherSettingsActivity.kt` | Grid size dialog (3x4, 4x5, 5x6) has no detail screenshot in reference set. | Marked **UNKNOWN**. Preserve existing grid selection dialog & Room persistence. |
| **Dock Settings Detail** | `LauncherSettingsActivity.kt` | Dock capacity / appearance dialog has no reference screenshot. | Marked **UNKNOWN**. Preserve existing dock configuration logic. |
| **Folder Window Detail** | `MainActivity.kt` (Folder overlay) | Folder popup/window dialog has no reference screenshot. | Marked **UNKNOWN**. Preserve existing folder popup & drag-drop logic. |
| **Gestures & Inputs Detail**| `GestureHandler.kt` | Gestures settings activity/dialog has no reference screenshot. | Marked **UNKNOWN**. Preserve existing swipe up/down, pinch, double tap gesture handlers. |
| **App Long-Press Popup Menu**| `MainActivity.kt`, `AppsAdapter.kt` | Long press app shortcut context menu has no reference screenshot. | Marked **UNKNOWN**. Preserve app uninstall, app info, drag-to-workspace actions. |
| **Widget Picker Dialog** | `WidgetPickerDialog.kt` | System widget picker / bounds clamping has no reference screenshot. | Marked **UNKNOWN**. Preserve existing AppWidgetHost picker & span calculation. |
| **Backup & Restore** | `LauncherSettingsActivity.kt` | Backup/restore dialog or screen has no reference screenshot. | Marked **UNKNOWN**. Preserve existing backup/restore intent handlers. |

---

## Required Implementation Roadmap (Step C Order)

1. **Phase 9.3R-B: Theme Foundation**
   - Implement `NovaDark` theme (setup wizard) and `NovaLight` theme (home, drawer, settings).
   - Define exact color, dimen, shape, and typography tokens matching extracted screenshot measurements.

2. **Phase 9.3R-C1: Settings Main (Ref 12, 13)**
   - Reskin `LauncherSettingsActivity` to light theme with large title, top search pill, red-outlined default launcher warning banner, 12 ordered settings rows with exact subtitles, and bottom navigation bar (Settings / Style).

3. **Phase 9.3R-C2: Home Screen (Ref 10)**
   - Reskin `MainActivity` home screen: transparent wallpaper background, remove clock & AI cards, 5-icon dock directly on wallpaper (no glass panel), bottom pale peach search bar (`Dock (Below icons)`).

4. **Phase 9.3R-C3: App Drawer (Ref 11)**
   - Reskin App Drawer to light `#FFFBFF` theme: 5-column grid, top search pill with 9-dot icon, suggestion chips, "No Frequent Apps" divider state, Smart Folders 2x2 preview tiles, right scroll thumb. Remove category chips & item counters.

5. **Phase 9.3R-C4 & C5: Setup Wizard & Icon Styling (Ref 01, 04, 05, 06, 07, 08)**
   - Implement `SetupActivity` (5-step onboarding flow + `MoreShapesDialogFragment` lavender modal).
   - Implement real icon sizing % and adaptive shape masking (Rounded square, Round, Squircle, Flower, Teardrop, Hexagon, Octagon, etc.).

6. **Phase 9.3R-C6: Default Launcher Flow (Ref 02, 09)**
   - Wire `RoleManager.createRequestRoleIntent(ROLE_HOME)` for native system default launcher dialog.
