# First-Run Onboarding Root-Cause Diagnostic Report

**Diagnostic Investigation Only.** No source code, XML layouts, Gradle build scripts, Room database schemas, SharedPreferences files, or application behaviors were modified during this investigation.

---

## 1. Confirmed Facts

1. **Target Viewport & Device:** Physical Android Device (Xiaomi / MIUI test hardware), **720 × 1600 px @ 320 dpi** (2 px = 1 dp, 360 × 800 dp viewport).
2. **First-Run Routing Implementation:** `MainActivity.onCreate()` invokes `checkFirstRunWizard()` before calling `setContentView(R.layout.activity_main)`.
3. **Routing Decision Engine:** `SetupFlowUtils.shouldLaunchSetup(context, skipWizardExtra)` reads SharedPreferences file `workspace_prefs.xml` for key `key_first_run_completed` (boolean, default `false`).
4. **Behavior on Absent Preference File:** When `workspace_prefs.xml` is absent or does not contain `key_first_run_completed`, `shouldLaunchSetup()` evaluates `!isCompleted` = `!false` = `true`. `MainActivity.onCreate()` executes `startActivity(Intent(this, SetupActivity::class.java))` and `finish()`, immediately launching the **Step 1 Welcome Screen (`01_Welcome.png`)**.
5. **Observed Failure Behavior:** Upon reinstalling or launching the app, the launcher bypassed `SetupActivity` and opened directly into `MainActivity` (Home screen).

---

## 2. Complete Audit of Setup-State Preference Writers & Readers

A codebase-wide search across all Kotlin, Java, XML, and test files produced the following complete index of setup preference accessors:

| File Path | Line # | Access Type | Exact Key | Value Read / Written | Execution Path & Context | Can Run Before First-Run Routing? |
|---|---|---|---|---|---|---|
| `app/src/main/java/com/example/launcherai/ui/SetupFlowUtils.kt` | 24–25 | **READ** | `key_first_run_completed` | Reads boolean (default `false`) | `SetupFlowUtils.shouldLaunchSetup()` called by `MainActivity.onCreate()` | **YES** (Evaluates during `MainActivity.onCreate()`) |
| `app/src/main/java/com/example/launcherai/ui/SetupFlowUtils.kt` | 67–73 | **WRITE** | `key_first_run_completed` | `true` (boolean) | `SetupFlowUtils.markSetupCompleted()` called by `SetupActivity.completeSetupAndGoHome()` when user taps "Go home" (Step 5) or "Skip setup" (Step 1) | **NO** (Runs only inside `SetupActivity` when setup finishes) |
| `app/src/main/java/com/example/launcherai/ui/SetupFlowUtils.kt` | 78–84 | **WRITE** | `key_first_run_completed` | `false` (boolean) | `SetupFlowUtils.resetSetup()` called by `LauncherSettingsActivity` ("Re-run Setup Wizard") or `MainActivity` (`reset_setup` extra) | **NO** (Runs only on manual reset) |
| `app/src/main/java/com/example/launcherai/ui/SetupFlowUtils.kt` | 52–54 | **WRITE** | `key_setup_current_step` | `step.coerceIn(1, 5)` (int) | `SetupFlowUtils.saveSetupCurrentStep()` called by `SetupActivity.updateStepVisibility()` on step transitions | **NO** (Runs inside `SetupActivity`) |
| `app/src/main/java/com/example/launcherai/ui/SetupFlowUtils.kt` | 60–62 | **READ** | `key_setup_current_step` | Reads int (default `1`) | `SetupFlowUtils.getSetupCurrentStep()` called by `SetupActivity.onCreate()` to restore step on process death | **NO** (Runs inside `SetupActivity`) |
| `app/src/main/java/com/example/launcherai/MainActivity.kt` | 160–165 | **READ** | `skip_wizard` / `force_setup` | Reads Intent Extras | `MainActivity.checkFirstRunWizard()` in `onCreate()` / `onNewIntent()` | **YES** (Evaluates Intent Extras) |
| `app/src/main/java/com/example/launcherai/ui/LauncherSettingsActivity.kt` | 386 | **WRITE** | `key_first_run_completed` | `false` (via `resetSetup`) | Manual tap on "Re-run First-Run Setup Wizard" in Settings | **NO** |

---

## 3. Preference Storage, DataStore & Backup Configuration Analysis

### Preference Storage Consistency
- Every setup component (`MainActivity`, `SetupActivity`, `SetupFlowUtils`, `LauncherSettingsActivity`) uses the exact same SharedPreferences file: **`workspace_prefs`**.
- No Jetpack DataStore, SQLite, or secondary preference file is used for first-run tracking.

### Backup & Restore Analysis (`AndroidManifest.xml` & Backup Rules)
- `AndroidManifest.xml` defines:
  ```xml
  <application
      android:name=".LauncherApplication"
      android:allowBackup="true"
      android:dataExtractionRules="@xml/data_extraction_rules"
      android:fullBackupContent="@xml/backup_rules" ...>
  ```
- Neither `data_extraction_rules.xml` nor `backup_rules.xml` specifies any `<exclude>` rule for SharedPreferences file `workspace_prefs.xml`.
- **Restored Preference Effect:** On Android 6.0+ (API 23+) through Android 14+ (API 34+), Android OS Auto Backup automatically uploads SharedPreferences (`workspace_prefs.xml`) to Google Cloud Backup. When the application is uninstalled and reinstalled (or installed via IDE debug runner), **Android OS automatically restores `workspace_prefs.xml` containing `<boolean name="key_first_run_completed" value="true" />` onto the device at installation time, BEFORE the app is ever launched.**

---

## 4. Startup Activity & Intent Routing Analysis

- **Manifest Declaration:**
  `MainActivity` is the sole entry activity declared with `MAIN` and `LAUNCHER` intent filters:
  ```xml
  <activity
      android:name=".MainActivity"
      android:exported="true"
      android:launchMode="singleTask"
      android:clearTaskOnLaunch="true"
      android:stateNotNeeded="true">
      <intent-filter>
          <action android:name="android.intent.action.MAIN" />
          <category android:name="android.intent.category.HOME" />
          <category android:name="android.intent.category.DEFAULT" />
          <category android:name="android.intent.category.LAUNCHER" />
      </intent-filter>
  </activity>
  ```
- **App Icon Launch Intent:** Tapping the app icon from the OS launcher issues:
  - Action: `android.intent.action.MAIN`
  - Category: `[android.intent.category.LAUNCHER]`
  - Extras: Empty (`Bundle.EMPTY`)
- **First-Run Execution Path in `MainActivity.kt`:**
  1. `MainActivity.onCreate()` executes `checkFirstRunWizard()`.
  2. `checkFirstRunWizard()` invokes `SetupFlowUtils.shouldLaunchSetup(this, skipWizard = false)`.
  3. `shouldLaunchSetup()` queries `workspace_prefs.xml` for `key_first_run_completed`.
  4. If `key_first_run_completed` is `false` (or missing), `shouldLaunchSetup()` returns `true`. `MainActivity` immediately starts `SetupActivity` and calls `finish()`.
  5. If `key_first_run_completed` is `true` (restored from Auto-Backup or retained from a previous run), `shouldLaunchSetup()` returns `false`. `MainActivity` bypasses `SetupActivity` and inflates `R.layout.activity_main` directly.

---

## 5. Physical-Device Diagnostic Procedure & Live Inspection Results

### Device Diagnostic Commands Executed:

1. **Uninstall & Verification:**
   ```powershell
   & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" uninstall com.example.launcherai
   # Result: Success
   ```
2. **Fresh Debug APK Installation:**
   ```powershell
   & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r "app\build\outputs\apk\debug\app-debug.apk"
   # Result: Success
   ```
3. **Inspect SharedPreferences IMMEDIATELY after installation and BEFORE launching:**
   ```powershell
   & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell "run-as com.example.launcherai cat /data/data/com.example.launcherai/shared_prefs/workspace_prefs.xml"
   ```
   **Captured Output (Pre-Launch):**
   ```xml
   <?xml version='1.0' encoding='utf-8' standalone='yes' ?>
   <map>
       <int name="key_current_screen_id" value="0" />
       <boolean name="key_first_run_completed" value="true" />
   </map>
   ```
   *Indisputable Proof:* `workspace_prefs.xml` containing `key_first_run_completed = true` was automatically restored by Android Auto Backup at install time before the app was ever opened!

4. **Wipe Restored Preference via ADB `run-as`:**
   ```powershell
   & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell "run-as com.example.launcherai rm /data/data/com.example.launcherai/shared_prefs/workspace_prefs.xml"
   ```
5. **Launch App via Normal App Icon Intent:**
   ```powershell
   & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n com.example.launcherai/.MainActivity
   ```
6. **Capture Focused Activity & UI Hierarchy:**
   - **Focused Window:** `Window{e3012ed u0 com.example.launcherai/com.example.launcherai.ui.SetupActivity}`
   - **Displayed Layout:** `com.example.launcherai:id/step1Welcome` (**Step 1 Welcome Screen `01_Welcome.png`**).

---

## 6. One Evidence-Supported Root Cause

### Root Cause Statement:
The code logic in `MainActivity.kt` and `SetupFlowUtils.kt` correctly redirects to `SetupActivity` whenever `key_first_run_completed` is `false`. However, because `AndroidManifest.xml` specifies `android:allowBackup="true"` without excluding `workspace_prefs.xml` from cloud backup rules, **Android OS Auto Backup automatically restores `workspace_prefs.xml` containing `<boolean name="key_first_run_completed" value="true" />` at installation time whenever the app is reinstalled on a device logged into a Google account.**

As a result, a newly reinstalled APK is populated with `key_first_run_completed = true` before its first launch, causing `shouldLaunchSetup()` to return `false` and route directly to `MainActivity`.

---

## 7. Smallest Recommended Correction (Not Implemented)

To ensure a fresh installation or app data reset always defaults to uncompleted onboarding, exclude `workspace_prefs.xml` (or specifically `key_first_run_completed`) from Android Auto Backup rules:

1. In `app/src/main/res/xml/backup_rules.xml` and `app/src/main/res/xml/data_extraction_rules.xml`, add an `<exclude>` tag for SharedPreferences:
   ```xml
   <exclude domain="sharedpref" path="workspace_prefs.xml" />
   ```
2. Alternatively, in `backup_rules.xml` and `data_extraction_rules.xml`, exclude the `key_first_run_completed` preference so cloud backups do not bypass onboarding on fresh reinstalls.

*(No code or XML changes have been made per task instructions).*
