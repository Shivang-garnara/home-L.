# LauncherAI Current Screenshot Inventory

This inventory documents all current LauncherAI application screenshots captured live on a physical test device (**720 × 1600 px @ 320 dpi**, 1 dp = 2 px, **360 × 800 dp** viewport) and stored under `docs/visual-audit/current/`.

---

## Device & Environment Specifications
- **Device Class:** Physical Android Device (Xiaomi / MIUI test hardware)
- **Physical Resolution:** 720 × 1600 px
- **Physical Density:** 320 dpi (density factor = 2.0)
- **Logical Viewport:** 360 × 800 dp
- **Storage Location:** `docs/visual-audit/current/`

---

## Screen Inventory Index

| # | Screen Identity | Reference File | Current Screenshot File | Capture Status | Viewport & File Size |
|---|---|---|---|---|---|
| **01** | Welcome | `Screenshot_2026-11-18-10-17-59-607_com.teslacoilsw.launcher.jpg` | `01_Welcome.png` | **Captured** | 720x1600 px (62.9 KB) |
| **02** | Default Chooser (Setup) | `Screenshot_2026-11-18-10-18-07-525_com.google.android.permissioncontroller.jpg` | `02_Default_chooser_setup.png` | **Captured** | 720x1600 px (62.9 KB) |
| **03** | Plan / Paywall | `Screenshot_2026-11-18-10-18-17-427_com.teslacoilsw.launcher.jpg` | *None* | **EXCLUDED** | Non-commercial free app; no paywall |
| **04** | Setup: Search | `Screenshot_2026-11-18-10-18-21-547_com.teslacoilsw.launcher.jpg` | `04_Setup_search.png` | **Captured** | 720x1600 px (110.5 KB) |
| **05** | Setup: App drawer | `Screenshot_2026-11-18-10-18-38-682_com.teslacoilsw.launcher.jpg` | `05_Setup_drawer.png` | **Captured** | 720x1600 px (103.9 KB) |
| **06** | Setup: Icon style | `Screenshot_2026-11-18-10-18-42-852_com.teslacoilsw.launcher.jpg` | `06_Setup_icon_style.png` | **Captured** | 720x1600 px (58.5 KB) |
| **07** | More shapes dialog | `Screenshot_2026-11-18-10-18-45-375_com.teslacoilsw.launcher.jpg` | `07_More_shapes_dialog.png` | **Captured** | 720x1600 px (62.9 KB) |
| **08** | All done! | `Screenshot_2026-11-18-10-18-51-077_com.teslacoilsw.launcher.jpg` | `08_All_done.png` | **Captured** | 720x1600 px (38.8 KB) |
| **09** | Default Chooser (Home) | `Screenshot_2026-11-18-10-18-55-306_com.google.android.permissioncontroller.jpg` | `09_Default_chooser_home.png` | **Captured** | 720x1600 px (38.8 KB) |
| **10** | Home Screen | `Screenshot_2026-11-18-10-19-01-046_com.teslacoilsw.launcher.jpg` | `10_Home.png` | **Captured** | 720x1600 px (200.3 KB) |
| **11** | App Drawer | `Screenshot_2026-11-18-10-19-08-010_com.teslacoilsw.launcher.jpg` | `11_App_drawer.png` | **Captured** | 720x1600 px (300.9 KB) |
| **12** | Settings Main | `Screenshot_2026-11-18-10-19-16-823_com.teslacoilsw.launcher.jpg` | `12_Settings_main.png` | **Captured** | 720x1600 px (111.7 KB) |
| **13** | Settings Scrolled | `Screenshot_2026-11-18-10-19-20-893_com.teslacoilsw.launcher.jpg` | `13_Settings_scrolled.png` | **Captured** | 720x1600 px (135.0 KB) |

---

## Screen Breakdown & Visual Contents

### 1. `01_Welcome.png`
- **File:** `docs/visual-audit/current/01_Welcome.png`
- **Background:** Dark background (`#151316`–`#211D1C`).
- **Elements Rendered:** Centered green launcher icon, "Welcome to" (16sp), "LauncherAI" title (32sp `sans-serif-light`), orange "Get started" pill button (`#D54526`, 118x40dp), bottom-left "Restore backup" link, bottom-right "Skip setup" link.

### 2. `02_Default_chooser_setup.png`
- **File:** `docs/visual-audit/current/02_Default_chooser_setup.png`
- **Background:** Dark setup background frame.
- **Elements Rendered:** System `RoleManager` default home app selection prompt.

### 3. `03_Paywall` (Excluded)
- **Status:** **EXCLUDED.** LauncherAI is an open-source, non-commercial launcher app with no subscriptions, in-app purchases, or paywalls.

### 4. `04_Setup_search.png`
- **File:** `docs/visual-audit/current/04_Setup_search.png`
- **Background:** Dark setup background frame, back arrow, 25% orange progress bar.
- **Elements Rendered:** "Search" title (28sp `sans-serif-light`), Mode dropdown ("Immersive ▾"), Shortcut Search toggle (ON), Contact Search toggle (OFF), Home search bar placement dropdown ("Dock (Below icons) ▾"), App drawer search placement dropdown ("Top ▾"), orange rounded-square next FAB (55x55dp, 18dp radius).

### 5. `05_Setup_drawer.png`
- **File:** `docs/visual-audit/current/05_Setup_drawer.png`
- **Background:** Dark setup background frame, back arrow, 50% orange progress bar.
- **Elements Rendered:** "App drawer" title, "App drawer style" dropdown ("Vertical ▾"), toggles ON for Show Keyboard, Frequently used apps, Frequently used shortcuts, Smart Folders, orange next FAB.

### 6. `06_Setup_icon_style.png`
- **File:** `docs/visual-audit/current/06_Setup_icon_style.png`
- **Background:** Dark setup background frame, back arrow, 75% orange progress bar.
- **Elements Rendered:** "Icon style" title, 4 color swatches (Cyan, White, Green, Magenta), "Icon size 125%", shape choices (Rounded square, Round, Squircle, Flower), "More" shapes button, orange next FAB.

### 7. `07_More_shapes_dialog.png`
- **File:** `docs/visual-audit/current/07_More_shapes_dialog.png`
- **Background:** Dimmed dark setup background.
- **Elements Rendered:** Pale lavender modal card (`#EDE5F4`, 312x418dp, 28dp radius), "More shapes" title (22sp `#231A24`), 4-column grid (Square, Teardrop, Pebble, Vessel, Pentagon, Hexagon, Hexagon 2nd variant, Heptagon, Octagon, Crop).

### 8. `08_All_done.png`
- **File:** `docs/visual-audit/current/08_All_done.png`
- **Background:** Dark setup background frame, back arrow, top orange checked circle icon.
- **Elements Rendered:** "All done!" title (28sp `sans-serif-light`), centered sparkle icon (`ic_sparkle_large`), "Setup is finished", full-width orange pill button "Go home" (`#D54526`, 328x48dp, 24dp radius).

### 9. `09_Default_chooser_home.png`
- **File:** `docs/visual-audit/current/09_Default_chooser_home.png`
- **Background:** Home screen / Settings light background.
- **Elements Rendered:** System `RoleManager` default home app chooser prompt.

### 10. `10_Home.png`
- **File:** `docs/visual-audit/current/10_Home.png`
- **Background:** Transparent system wallpaper (`#00000000`).
- **Elements Rendered:** Workspace shortcuts with 12sp white text labels, 5-icon dock directly on wallpaper (72dp height, NO labels, NO glass panel), pale peach search pill (`#FEDACE`, 340x55dp, 28dp radius) BELOW dock with 9-dot grid icon left & mic icon right.

### 11. `11_App_drawer.png`
- **File:** `docs/visual-audit/current/11_App_drawer.png`
- **Background:** Light solid background `#FFFBFF`.
- **Elements Rendered:** Top pale peach search pill ("Search apps", `#FEDBCF`, 328x44dp, 22dp radius) with 9-dot grid icon, "LauncherAI Settings" rounded suggestion chip, "No Frequent Apps" empty state + thin divider line `#D8C5C0`, 5-column app grid, right vertical scrollbar thumb (`#7D5245`).

### 12. `12_Settings_main.png`
- **File:** `docs/visual-audit/current/12_Settings_main.png`
- **Background:** Light warm surface `#FFF8F6`.
- **Elements Rendered:** Title "LauncherAI" (32sp `sans-serif-light`), pale pink search pill ("Search", `#F5E0DA`, 48dp height), red-outlined warning banner ("Not set as default", 2dp red border `#BF0100`, `#FFF8F6` fill), ordered rows (AI assistant, Home screen, App drawer, Folders), bottom nav bar (Settings [selected pill indicator `#F4EFF3`] and Style tabs).

### 13. `13_Settings_scrolled.png`
- **File:** `docs/visual-audit/current/13_Settings_scrolled.png`
- **Background:** Light warm surface `#FFF8F6`.
- **Elements Rendered:** Top search pill, lower ordered rows (Search, Cards, Look & feel, Gestures & inputs, Feed page, Integrations, Notification badges, Backup & restore), bottom nav bar.
