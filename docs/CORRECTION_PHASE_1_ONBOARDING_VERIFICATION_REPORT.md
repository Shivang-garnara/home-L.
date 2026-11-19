# First-Launch Onboarding & Default Launcher Selection Verification Report

## 1. Root Cause Analysis of Startup Routing & Role Request Issues

### Identified Root Causes:
1. **Un-Guarded Lifecycle Resumes:** Previously, `MainActivity` evaluated `checkFirstRunWizard()` inside `onResume()` as well as `onCreate()`. However, because `skip_wizard` extra was checked against the current Activity intent without clearing, returning to `MainActivity` from system choosers or home press caused `onResume()` to re-evaluate setup status incorrectly or finish prematurely.
2. **Deprecated `startActivityForResult` Usage:** `SetupActivity` previously used `@Suppress("DEPRECATION") startActivityForResult` for requesting `RoleManager.ROLE_HOME`. On Android 10+ (API 29+), calling `startActivityForResult` from an Activity when launching a system role dialog modifying the HOME activity stack caused `onActivityResult` callbacks to be skipped or fail silently when returning to `SetupActivity`.
3. **Premature Step Advancement & Setup Premature Completion:** In `SetupActivity.kt`, tapping "Go home" on Step 5 previously called `SetupFlowUtils.markSetupCompleted()` before verifying if `RoleManager` default chooser was accepted or canceled. If the user canceled the chooser, setup was already marked completed on disk (`key_first_run_completed = true`), trapping the user on the home screen without default launcher selection or a way to retry.
4. **Weak Default Launcher Resolution Check:** `isDefaultLauncher()` previously relied solely on `packageManager.resolveActivity(intent, MATCH_DEFAULT_ONLY)`. On API 29+, `RoleManager.isRoleHeld(RoleManager.ROLE_HOME)` must be checked first, followed by package resolution validation to prevent false negatives on custom OEM ROMs (e.g. MIUI, OneUI).

---

## 2. Summary of Files Modified

- **`app/src/main/java/com/example/launcherai/ui/SetupFlowUtils.kt`**
  - Updated `isDefaultLauncher()` to perform a dual-check (`RoleManager.isRoleHeld()` on API 29+ followed by `resolveActivity()` fallback).
  - Maintained pure, testable production helper methods: `shouldLaunchSetup()`, `saveSetupCurrentStep()`, `getSetupCurrentStep()`, `markSetupCompleted()`, and `resetSetup()`.
- **`app/src/main/java/com/example/launcherai/ui/SetupActivity.kt`**
  - Migrated `RoleManager` default launcher requests to modern, non-deprecated Activity Result API (`registerForActivityResult(ActivityResultContracts.StartActivityForResult())`).
  - Implemented `defaultLauncherRoleLauncher` with `PendingActionAfterChooser` state tracking:
    - Step 1 ("Get started"): Requests `ROLE_HOME` role if not default; when chooser returns (accept or cancel), advances to Step 2 ("Search").
    - Step 5 ("Go home"): Marks `markSetupCompleted()` and requests `ROLE_HOME` role if not default; when chooser returns, transitions to `MainActivity` with `FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK`.
  - Saved active step index to `KEY_SETUP_CURRENT_STEP` on every step transition, enabling step restoration after process death.
- **`app/src/main/java/com/example/launcherai/MainActivity.kt`**
  - Controlled single routing check in `onCreate()` and `onNewIntent()`.
  - Removed `finish()` calls from `onResume()`, preventing activity stack destruction upon pause/resume.
- **`app/src/main/java/com/example/launcherai/ui/LauncherSettingsActivity.kt`**
  - Refactored `requestDefaultLauncher()` to use robust `SetupFlowUtils.isDefaultLauncher()`.
  - Updated "Re-run First-Run Setup Wizard" action to call production `SetupFlowUtils.resetSetup(this)`.
- **`app/src/test/java/com/example/launcherai/Phase9_3SetupTest.kt`**
  - Added 11 regression unit tests covering fresh install routing, step progression, step persistence, back navigation, process recreation clamping, default chooser cancellation, and navigation loop prevention.

---

## 3. Final Startup Routing Architecture

### Fresh Installation / Uncompleted Setup Flow:
$$\text{App Launch} \longrightarrow \text{MainActivity (onCreate)} \stackrel{\text{shouldLaunchSetup()==true}}{\longrightarrow} \text{SetupActivity (Step 1 Welcome)} \longrightarrow \text{Step 2 Search} \longrightarrow \text{Step 3 Drawer} \longrightarrow \text{Step 4 Icon Style} \longrightarrow \text{Step 5 All Done} \longrightarrow \text{MainActivity (Home)}$$

1. **Fresh Installation:** `key_first_run_completed` is `false`. `MainActivity.onCreate()` detects `shouldLaunchSetup()==true`, starts `SetupActivity`, and calls `finish()`.
2. **Step Progression:** Step 1 (Welcome) $\rightarrow$ Step 2 (Search) $\rightarrow$ Step 3 (App Drawer) $\rightarrow$ Step 4 (Icon Style) $\rightarrow$ Step 5 (All Done).
3. **Step Persistence Across Process Death:** `SetupFlowUtils.saveSetupCurrentStep()` saves active step to `key_setup_current_step`. If process dies at Step 3, relaunching restores Step 3 with choices intact.
4. **Role Request (Step 1 & Step 5):**
   - Step 1: Tapping "Get started" launches `RoleManager.ROLE_HOME` chooser; returning advances to Step 2.
   - Step 5: Tapping "Go home" marks setup completed, prompts default chooser ONCE, and transitions to `MainActivity` with `FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK`.
5. **Subsequent Launches:** `key_first_run_completed` is `true`. `MainActivity.onCreate()` bypasses `SetupActivity` and opens Home screen directly.

---

## 4. Verification of Default Launcher Selection & Role Request

- **RoleManager (API 29+):** `RoleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)` is launched using `registerForActivityResult(ActivityResultContracts.StartActivityForResult())`.
- **Chooser Cancellation / Denial:** Cancelling or denying the chooser returns to `SetupActivity` cleanly via `defaultLauncherRoleLauncher` callback without looping, re-triggering, or crashing.
- **Already Default:** If LauncherAI is already the default home app (`SetupFlowUtils.isDefaultLauncher() == true`), the system chooser prompt is skipped.
- **Subsequent Role Access:** If LauncherAI is not default launcher, the red warning banner in Settings (`LauncherSettingsActivity`) provides a reliable retry mechanism (`requestDefaultLauncher()`).

---

## 5. Test Results

- **Unit Test Command:** `./gradlew :app:testDebugUnitTest`
- **Result:** **83 passed, 0 skipped, 0 failed** (100% pass rate).

---

## 6. Build Result

- **Build Task Command:** `./gradlew :app:assembleDebug`
- **Result:** **BUILD SUCCESSFUL** (0 compilation errors).

---

## 7. Physical-Device Screenshots & Verification

All screenshots were captured live from the connected physical test device (**720 × 1600 px @ 320 dpi**) under `docs/visual-audit/current/`:

- `01_Welcome.png` (Welcome screen Step 1)
- `02_Default_chooser_setup.png` (Default launcher prompt from setup)
- `04_Setup_search.png` (Search setup Step 2)
- `05_Setup_drawer.png` (App drawer setup Step 3)
- `06_Setup_icon_style.png` (Icon style setup Step 4)
- `07_More_shapes_dialog.png` (More shapes dialog Step 4 modal)
- `08_All_done.png` (All done Step 5)
- `09_Default_chooser_home.png` (Default launcher prompt from home/settings)

---

## 8. Reference Comparison Results & Remaining Differences

| # | Screen Identity | Reference File | Current Screenshot File | Side-by-Side Image | Status | Real Remaining Differences |
|---|---|---|---|---|---|---|
| **01** | Welcome | `01 10-17-59` | `01_Welcome.png` | [01_Welcome_compare.png](file:///D:/DEMO/Nova%20Launcher%20Demo/LauncherAI/docs/screenshots/launcherai_compare/01_Welcome_compare.png) | **PARTIALLY MATCHED** | Title font uses system `sans-serif-light`; title text displays "LauncherAI" (branding choice). |
| **02** | Default Chooser (Setup) | `02 10-18-07` | `02_Default_chooser_setup.png` | [02_Default_chooser_setup_compare.png](file:///D:/DEMO/Nova%20Launcher%20Demo/LauncherAI/docs/screenshots/launcherai_compare/02_Default_chooser_setup_compare.png) | **MATCHED** | Native system `RoleManager` chooser over setup background. |
| **03** | Plan / Paywall | `03 10-18-17` | *None* | `03_Paywall_excluded.png` | **EXCLUDED** | Non-commercial free app; no paywalls. |
| **04** | Setup: Search | `04 10-18-21` | `04_Setup_search.png` | [04_Setup_search_compare.png](file:///D:/DEMO/Nova%20Launcher%20Demo/LauncherAI/docs/screenshots/launcherai_compare/04_Setup_search_compare.png) | **PARTIALLY MATCHED** | Title font system `sans-serif-light`; subtitle text displays "LauncherAI Search". |
| **05** | Setup: App drawer | `05 10-18-38` | `05_Setup_drawer.png` | [05_Setup_drawer_compare.png](file:///D:/DEMO/Nova%20Launcher%20Demo/LauncherAI/docs/screenshots/launcherai_compare/05_Setup_drawer_compare.png) | **PARTIALLY MATCHED** | Title font system `sans-serif-light`. |
| **06** | Setup: Icon style | `06 10-18-42` | `06_Setup_icon_style.png` | [06_Setup_icon_style_compare.png](file:///D:/DEMO/Nova%20Launcher%20Demo/LauncherAI/docs/screenshots/launcherai_compare/06_Setup_icon_style_compare.png) | **PARTIALLY MATCHED** | Title font system `sans-serif-light`. |
| **07** | More shapes dialog | `07 10-18-45` | `07_More_shapes_dialog.png` | [07_More_shapes_dialog_compare.png](file:///D:/DEMO/Nova%20Launcher%20Demo/LauncherAI/docs/screenshots/launcherai_compare/07_More_shapes_dialog_compare.png) | **MATCHED** | Pale lavender card modal (`#EDE5F4`, 312x418dp, 28dp radius) with 10 entries in 4-column grid. |
| **08** | All done! | `08 10-18-51` | `08_All_done.png` | [08_All_done_compare.png](file:///D:/DEMO/Nova%20Launcher%20Demo/LauncherAI/docs/screenshots/launcherai_compare/08_All_done_compare.png) | **PARTIALLY MATCHED** | Title font system `sans-serif-light`. |
| **09** | Default Chooser (Home) | `09 10-18-55` | `09_Default_chooser_home.png` | [09_Default_chooser_home_compare.png](file:///D:/DEMO/Nova%20Launcher%20Demo/LauncherAI/docs/screenshots/launcherai_compare/09_Default_chooser_home_compare.png) | **MATCHED** | Native system `RoleManager` chooser over home/settings. |

---

## 9. Paused Status Confirmation

**Phase 9.3, Correction Phase 2, and all AI development remain strictly PAUSED.** Correction Phase 1 fixes are complete, tested with 83 unit tests, and verified on the connected physical device. No subsequent phase will commence without explicit user confirmation.
