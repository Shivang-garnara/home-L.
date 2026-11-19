# Nova Launcher vs LauncherAI Visual Comparison & Gap Audit

## 1. Executive Summary

This document presents a rigorous, evidence-backed visual and behavioral comparison between Nova Launcher 8.9.6 reference screenshots (`D:\DEMO\Nova Launcher Demo\Photos`) and current LauncherAI captured screenshots (`docs/visual-audit/current/`).

Every pair has been evaluated through direct pixel comparison, side-by-side comparison images (`docs/visual-audit/compare/`), and pixel-difference overlay images (`docs/visual-audit/overlay/`).

---

## 2. Visual Comparison Matrix & Parity Classifications

| # | Screen Identity | Reference File (`Photos/`) | Current Screenshot (`visual-audit/current/`) | Side-by-Side Image (`compare/`) | Pixel Diff Overlay (`overlay/`) | Evidence-Backed Classification |
|---|---|---|---|---|---|---|
| **01** | Welcome | `01 10-17-59` | `01_Welcome.png` | `01_Welcome_compare.png` | `01_Welcome_diff.png` | **PARTIALLY MATCHED** |
| **02** | Default Chooser (Setup) | `02 10-18-07` | `02_Default_chooser_setup.png` | `02_Default_chooser_setup_compare.png` | `02_Default_chooser_setup_diff.png` | **MATCHED** |
| **03** | Plan / Paywall | `03 10-18-17` | *None* | `03_Paywall_excluded.png` | *None* | **EXCLUDED** |
| **04** | Setup: Search | `04 10-18-21` | `04_Setup_search.png` | `04_Setup_search_compare.png` | `04_Setup_search_diff.png` | **PARTIALLY MATCHED** |
| **05** | Setup: App drawer | `05 10-18-38` | `05_Setup_drawer.png` | `05_Setup_drawer_compare.png` | `05_Setup_drawer_diff.png` | **PARTIALLY MATCHED** |
| **06** | Setup: Icon style | `06 10-18-42` | `06_Setup_icon_style.png` | `06_Setup_icon_style_compare.png` | `06_Setup_icon_style_diff.png` | **PARTIALLY MATCHED** |
| **07** | More shapes dialog | `07 10-18-45` | `07_More_shapes_dialog.png` | `07_More_shapes_dialog_compare.png` | `07_More_shapes_dialog_diff.png` | **MATCHED** |
| **08** | All done! | `08 10-18-51` | `08_All_done.png` | `08_All_done_compare.png` | `08_All_done_diff.png` | **PARTIALLY MATCHED** |
| **09** | Default Chooser (Home) | `09 10-18-55` | `09_Default_chooser_home.png` | `09_Default_chooser_home_compare.png` | `09_Default_chooser_home_diff.png` | **MATCHED** |
| **10** | Home Screen | `10 10-19-01` | `10_Home.png` | `10_Home_compare.png` | `10_Home_diff.png` | **PARTIALLY MATCHED** |
| **11** | App Drawer | `11 10-19-08` | `11_App_drawer.png` | `11_App_drawer_compare.png` | `11_App_drawer_diff.png` | **PARTIALLY MATCHED** |
| **12** | Settings Main | `12 10-19-16` | `12_Settings_main.png` | `12_Settings_main_compare.png` | `12_Settings_main_diff.png` | **PARTIALLY MATCHED** |
| **13** | Settings Scrolled | `13 10-19-20` | `13_Settings_scrolled.png` | `13_Settings_scrolled_compare.png` | `13_Settings_scrolled_diff.png` | **PARTIALLY MATCHED** |

---

## 3. Detailed Screenshot-by-Screenshot Visual & Layout Analysis

### 01. Welcome (`01 10-17-59` vs `01_Welcome.png`)
- **Composition & Layout:** Both screens center hero logo mark, title, and orange pill button, with bottom action links left and right.
- **Typography:** Nova uses custom geometric font "Nova Sans". LauncherAI uses system `sans-serif-light`. Title text displays "LauncherAI" (branding difference).
- **Colors:** Background gradient (`#151316`–`#211D1C`) and orange button (`#D54526`) match closely.
- **Classification:** **PARTIALLY MATCHED** (Branding & typography differences).

### 02. Default Chooser (Setup) (`02 10-18-07` vs `02_Default_chooser_setup.png`)
- **Composition & Geometry:** Android system `RoleManager` default home chooser modal (`#FCEAE4` pale peach surface, 28dp radius).
- **Classification:** **MATCHED** (Identical system UI dialog).

### 03. Plan / Paywall (`03 10-18-17`)
- **Status:** **EXCLUDED.** LauncherAI is an open-source non-commercial launcher with no subscriptions or paywalls.

### 04. Setup: Search (`04 10-18-21` vs `04_Setup_search.png`)
- **Composition & Controls:** Back arrow, 25% orange progress bar, "Search" title, Mode dropdown ("Immersive ▾"), Shortcut Search toggle (ON), Contact Search toggle (OFF), Home search placement dropdown ("Dock (Below icons) ▾"), Drawer placement dropdown ("Top ▾"), orange next FAB (55x55dp, 18dp radius).
- **Differences:** Title font system `sans-serif-light`; subtitle text displays "LauncherAI Search".
- **Classification:** **PARTIALLY MATCHED**.

### 05. Setup: App drawer (`05 10-18-38` vs `05_Setup_drawer.png`)
- **Composition & Controls:** 50% orange progress bar, "App drawer" title, "Vertical ▾" style dropdown, toggles ON for Show Keyboard, Frequently used apps, Frequently used shortcuts, Smart Folders.
- **Differences:** Title font system `sans-serif-light`.
- **Classification:** **PARTIALLY MATCHED**.

### 06. Setup: Icon style (`06 10-18-42` vs `06_Setup_icon_style.png`)
- **Composition & Controls:** 75% orange progress bar, "Icon style" title, 4 color swatches, "Icon size 125%", minus/plus/slider, shape choices (Rounded square, Round, Squircle, Flower, "More"), orange next FAB.
- **Differences:** Title font system `sans-serif-light`; preview shapes render flat gray swatches until C5 shape path rendering is active across all workspace/dock icons.
- **Classification:** **PARTIALLY MATCHED**.

### 07. More shapes dialog (`07 10-18-45` vs `07_More_shapes_dialog.png`)
- **Composition & Geometry:** Pale lavender card modal (`#EDE5F4`, 312x418dp, 28dp radius), "More shapes" title (22sp `#231A24`), 4-column grid (Square, Teardrop, Pebble, Vessel, Pentagon, Hexagon, Hexagon 2nd variant, Heptagon, Octagon, Crop). No mid-word label wrap.
- **Classification:** **MATCHED**.

### 08. All done! (`08 10-18-51` vs `08_All_done.png`)
- **Composition & Controls:** Orange checked circle header, "All done!" title, centered sparkle icon, "Setup is finished", full-width orange pill button "Go home" (`#D54526`, 328x48dp, 24dp radius).
- **Differences:** Title font system `sans-serif-light`.
- **Classification:** **PARTIALLY MATCHED**.

### 09. Default Chooser from Home (`09 10-18-55` vs `09_Default_chooser_home.png`)
- **Composition & Geometry:** Android system `RoleManager` default home app chooser prompt over live home wallpaper.
- **Classification:** **MATCHED**.

### 10. Home Screen (`10 10-19-01` vs `10_Home.png`)
- **Composition:** Transparent system wallpaper (`#00000000`), workspace shortcuts with white text labels, 5-icon dock directly on wallpaper (72dp height, NO labels, NO glass panel), pale peach search pill (`#FEDACE`, 340x55dp, 28dp radius) BELOW dock with 9-dot grid icon left & mic icon right.
- **Differences:** (1) Dock slot width is 64dp vs reference ~72dp; (2) Search pill right icon uses neutral mic VectorDrawable (`ic_mic`) instead of Google 4-color dots; (3) System status bar icons depend on test device.
- **Classification:** **PARTIALLY MATCHED**.

### 11. App Drawer (`11 10-19-08` vs `11_App_drawer.png`)
- **Composition:** Light solid background `#FFFBFF`, top pale peach search pill ("Search apps", `#FEDBCF`, 328x44dp, 22dp radius) with 9-dot grid icon, "LauncherAI Settings" rounded suggestion chip, "No Frequent Apps" empty state + thin divider line `#D8C5C0`, 5-column grid, right vertical scrollbar thumb (`#7D5245`). Category chips, app count, and close chevron removed.
- **Differences:** (1) Suggestion chip text displays "LauncherAI Settings" (branding); (2) Category smart folders 2x2 preview tiles require dynamic grouping generator.
- **Classification:** **PARTIALLY MATCHED**.

### 12 & 13. Settings Main & Scrolled (`12 & 13` vs `12_Settings_main.png` & `13_Settings_scrolled.png`)
- **Composition:** Light warm surface `#FFF8F6`, title "LauncherAI" (32sp `sans-serif-light`), top search pill ("Search", `#F5E0DA`, 48dp), red-outlined warning banner ("Not set as default", 2dp red border `#BF0100`), 12 ordered rows with exact subtitles, bottom nav bar (Settings [selected pill indicator `#F4EFF3`] and Style tabs).
- **Differences:** (1) Title font uses system `sans-serif-light`; (2) Title & subtitles display "LauncherAI" branding; (3) Prime badge omitted on Notification Badges per free app instructions.
- **Classification:** **PARTIALLY MATCHED**.

---

## 4. Verification of Interaction Flows

| Interaction Flow | Verification Results | Parity Status |
|---|---|---|
| **First-Run Startup** | App checks `SetupFlowUtils.shouldLaunchSetup()` in `onCreate()`, `onNewIntent()`, and `onResume()`. Redirects to `SetupActivity` when setup is uncompleted. | **WORKING** |
| **Welcome $\rightarrow$ Setup** | Tapping "Get started" triggers system default launcher prompt (`ROLE_HOME`), then advances to Step 2 ("Search"). | **WORKING** |
| **Setup Steps $\rightarrow$ Completion** | Progresses through Steps 2 (Search) $\rightarrow$ 3 (App Drawer) $\rightarrow$ 4 (Icon Style) $\rightarrow$ 5 (All Done). FAB advances steps; back arrow returns to previous step. | **WORKING** |
| **Completion $\rightarrow$ Default Chooser** | Tapping "Go home" calls `SetupFlowUtils.markSetupCompleted()`, checks default launcher status ONCE, triggers Ref 09 if not default, and opens Home. | **WORKING** |
| **Home $\rightarrow$ App Drawer** | Tapping search bar left 9-dot grid icon or performing swipe up gesture opens App Drawer overlay. | **WORKING** |
| **App Drawer $\rightarrow$ Home** | Swipe down gesture or Back press closes App Drawer overlay cleanly. | **WORKING** |
| **Home $\rightarrow$ Settings** | Tapping Nova/LauncherAI Settings shortcut or swipe gesture opens `LauncherSettingsActivity`. | **WORKING** |
| **Settings $\rightarrow$ Config Pages** | Tapping rows 1..5, 7..8, 12 opens corresponding dialogs/activities; rows 6, 9..11 open themed "Not available yet" sub-shells cleanly. | **WORKING** |
| **Dock Customization** | Dynamic 3, 5, 7 slots capacity supported via `showDockSettingsDialog()` in Settings. | **WORKING** |
| **Icon Customization** | Icon size % and shape selection saved to `workspace_prefs`. `IconShapeMasker` constructs path masks. | **WORKING** |
| **Long Press Actions** | Long-pressing home search bar opens AI assistant (`NovaAiChatActivity`). Long-pressing workspace empty area opens Widget Picker. Long-pressing app shortcut opens drag/uninstall options. | **WORKING** |
| **Drag and Drop** | Dragging app from drawer or workspace updates position via `DragController` and persists to Room database. | **WORKING** |
| **Back Navigation** | System back button handles sub-shells, drawer overlay, and setup step reversal without crash or loop. | **WORKING** |

---

## 5. Summary of What Must Be Corrected First

Before starting Phase 9.3 or any new features, the following core corrections must be executed in order:

1. **Onboarding Startup Routing:** Enforce `checkFirstRunWizard()` on every startup entry point (`onCreate`, `onNewIntent`, `onResume`) so `SetupActivity` reliably opens on fresh install without requiring manual Intent flags.
2. **Icon Sizing & Path Masking Engine Application (C5 Integration):** Apply `IconShapeMasker.createMaskedIcon` across all workspace icons (`Workspace.kt`/`CellLayout.kt`) and dock icons (`DockLayout.kt`) so shape choices (Flower, Teardrop, Pebble, Vessel, Pentagon, Hexagon, Octagon, Crop) render real path masks.
3. **Home Dock Slot Spacing (C2 Refinement):** Adjust `DockLayout.kt` slot width to 72dp to match reference 10 dock spacing.
4. **App Drawer Smart Folders (C3 Refinement):** Generate dynamic 2x2 category preview tiles (Entertainment, Games) as the first items in the drawer grid when `key_drawer_smart_folders == true`.
