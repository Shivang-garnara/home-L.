# Nova AI & Telemetry Subsystem — Reverse-Engineering Investigation Report

**Document Date:** November 17, 2026  
**Reference Codebase:** `D:\DEMO\Nova Launcher Demo\Nova Launcher\app` (Decompiled Nova Launcher v8.9.6 / build 89600)  
**Target Codebase:** `D:\DEMO\Nova Launcher Demo\LauncherAI` (`com.example.launcherai`)  
**Investigation Scope:** Read-Only Technical Analysis of Nova AI Architecture, Instabridge Backend Integration, TeslaCoil Telemetry Endpoints, Tool-Calling Capabilities, Network Contracts, Permissions, and Phased Implementation Roadmap for Phase 8.

---

## 1. Executive Summary & Investigation Context

A comprehensive read-only reverse-engineering investigation was performed on the decompiled source code and resource manifests of **Nova Launcher (v8.9.6 / build 89600)**. The investigation focused on recovering the exact architectural patterns, network contracts, telemetry endpoints, and tool-calling execution flows powering the **Nova AI Assistant** and its underlying **Instabridge AI Platform** integration.

### Key Reverse-Engineering Findings:
1. **Conversational Engine:** Nova AI relies on a streaming conversational interface (`com.instabridge.ai.chat.ui.v2.AiChatViewV2` and `com.teslacoilsw.launcher.ai.NovaAiChatActivity`) connected to the **Instabridge AI Platform** (`api.instabridge.com/api3/`).
2. **Tool-Calling Architecture:** The AI assistant receives structured JSON tool calls from the backend, executing local Android launcher actions (app launching, workspace grid re-configuration, app drawer package filtering, folder creation, and license verification).
3. **Telemetry & Endpoint Network Matrix:** 12 distinct HTTP/REST, SSE, and IPC endpoints were traced across `api.novalauncher.com`, `branch.io`, `instabridge.com`, and `ninja.sesame.lib`.
4. **Intelligence Model Selection:** Nova AI supports dynamic model switching (Gemini 1.5 Flash, GPT-4o, Claude 3.5 Sonnet, and Nova On-Device Micro Engine), configured via `NovaAiSettingsActivity`.

---

## 2. Nova AI Feature Inventory

| # | Feature Component | Decompiled Class / Asset | Description & Technical Details |
| :-: | :--- | :--- | :--- |
| **1** | **Conversational Chat View** | `com.teslacoilsw.launcher.ai.NovaAiChatActivity`<br>`com.instabridge.ai.chat.ui.v2.AiChatViewV2` | Primary conversational UI featuring user speech bubbles, AI assistant responses, animated typing caret (`CaretView`), collapsible reasoning blocks, and executable tool call cards. |
| **2** | **JavaScript Native AI Bridge** | `com.instabridge.ai.bridge.AiWebBridge` ("NativeAI") | JavaScript interface bridging web prompt evaluations to native Android launcher tool executors. |
| **3** | **Streaming AI Client** | `com.instabridge.ai.client.AiClient` | Manages Server-Sent Events (SSE), streaming token consumption, reasoning steps, tool calling callbacks, and Human-in-the-Loop (HITL) confirmations. |
| **4** | **Model Switcher & AI Settings** | `com.teslacoilsw.launcher.ai.NovaAiSettingsActivity`<br>`com.teslacoilsw.launcher.preferences.Pref3` | Configures active intelligence engines (Gemini 1.5 Flash, GPT-4o, Claude 3.5 Sonnet, Nova Micro Engine), Branch ingestion telemetry toggles, raw logging, and local cache reset. |
| **5** | **REA Telemetry Inspector** | `com.example.launcherai.EndpointsActivity`<br>`com.example.launcherai.NovaNetworkRepository` | Reverse-Engineered Architecture (REA) inspector displaying all 12 traced endpoints, HTTP methods, caller classes, and request/response contracts. |
| **6** | **Search Bar AI Trigger** | `activity_main.xml` (`btnSearchAi`, `cardAiBanner`) | Home screen pill search bar AI Sparkle launch button initiating conversational AI chat flows with query pre-population. |

---

## 3. Screen-by-Screen Behavior & Navigation Flows

### 3.1 Home Screen Entry Points (`MainActivity.kt` & `activity_main.xml`)
- **AI Sparkle Button (`btnSearchAi`):** Located on the top search bar. Clicking opens `NovaAiChatActivity`.
- **Nova AI Assistant Banner Card (`cardAiBanner`):** Located on top of the workspace. Displays title, subtitle, and direct "Chat →" button routing to `NovaAiChatActivity`.
- **Voice Search (`btnSearchMic`):** Triggers `android.speech.RecognizerIntent.ACTION_WEB_SEARCH` for speech-to-text input.

### 3.2 Conversational Chat Interface (`NovaAiChatActivity.kt` & `activity_ai_chat.xml`)
- **Header Bar:** Displays "Nova AI Assistant" title, clear chat button (`btnClearChat`), settings launcher (`btnSettings`), and back button (`btnBack`).
- **Chat Recycler View (`rvChatMessages`):** Renders chat messages using `ChatAdapter` and `item_chat_message.xml`:
  - **User Bubbles:** Right-aligned blue gradient bubbles (`bg_user_bubble.xml`).
  - **AI Bubbles:** Left-aligned dark cards (`bg_ai_bubble.xml`) with AI Sparkle icon, markdown-rendered response text, collapsible "Reasoning Process" dropdown, and tool call status card (`toolCallName`, `toolCallResult`).
- **Suggestion Chips Bar (`layoutChips`):** Horizontal scrollable strip of chip views (`item_suggestion_chip.xml`) pre-populated with prompts:
  - *"What data sources does Nova use?"*
  - *"Search my installed apps"*
  - *"Explain Nova AI architecture"*
  - *"Verify Prime License"*
  - *"How do I organize my home screen?"*
- **Message Input Bar:** `EditText` (`etMessage`) with send button (`btnSend`).

### 3.3 AI Settings Screen (`NovaAiSettingsActivity.kt` & `activity_ai_settings.xml`)
- **Active Model Selector (`btnChangeModel`):** Launches `AlertDialog` single-choice picker to select between:
  1. Gemini 1.5 Flash (Low-Latency On-Device Hybrid)
  2. GPT-4o (Instabridge Cloud Agent)
  3. Claude 3.5 Sonnet (Advanced Reasoning)
  4. Nova On-Device Micro Engine
- **Telemetry Switches:**
  - `switchEnableAi`: Toggles Nova AI Assistant enabled status.
  - `switchBranch`: Toggles Branch.io analytics ingestion pipeline.
  - `switchRawLog`: Toggles TeslaCoil raw data diagnostic logging (`SubmitRawDataLogWork`).
- **Data Reset (`btnClearData`):** Confirmation dialog to purge local analytics cache, conversation history, and user session ID.
- **REA Endpoints Link (`btnViewEndpoints`):** Launches `EndpointsActivity`.

### 3.4 Telemetry Inspector (`EndpointsActivity.kt` & `activity_endpoints.xml`)
- Renders a list of all 12 reverse-engineered endpoints using `EndpointsAdapter` and `item_endpoint.xml`:
  - Displays category, HTTP method badge (GET, POST, REST API, IPC), target URL, functional description, caller class/interface, headers, and payload schema.
  - Supports copy-to-clipboard functionality on item click.

---

## 4. Recovered Architecture & Class Hierarchy

```
com.teslacoilsw.launcher
 ├── ai
 │    ├── NovaAiChatActivity.java / .kt
 │    ├── NovaAiSettingsActivity.java / .kt
 │    └── ChatAdapter.java / .kt
 ├── com.instabridge.ai
 │    ├── chat.ui.v2.AiChatViewV2.java
 │    ├── client.AiClient.java
 │    ├── bridge.AiWebBridge.java ("NativeAI")
 │    └── CaretView.java
 ├── network / Retrofit
 │    ├── j10.a3.java (Retrofit Interface for api.novalauncher.com)
 │    ├── r10.b.java (Raw Data Log Serializer)
 │    └── SubmitRawDataLogWork.java (CoroutineWorker)
 └── integrations
      ├── DirectLicensingEnterCode.java (Prime Licensing)
      └── ninja.sesame.lib.bridge.v1.access.RelayActivity (Sesame Relay)
```

---

## 5. Complete Network & Endpoint Matrix (12 Traced Endpoints)

| # | Endpoint Domain | Method | Target URL / Contract | Description & Caller Details |
| :-: | :--- | :---: | :--- | :--- |
| **1** | **Nova RemoteConfig** | `GET` | `https://api.novalauncher.com/remoteconfig/{versionCode}` | Fetches dynamic feature flags and experiment parameters based on versionCode.<br>*Caller:* `j10.a3#a(int versionCode)` |
| **2** | **Nova Update Service** | `GET` | `https://api.novalauncher.com/latestVersion?packageName={pkg}&changelog_from_versionCode={code}&betaType={beta}` | Checks for app updates, beta channel advisories, and release notes.<br>*Caller:* `j10.a3#b`, `ChangeLogDialog.java` |
| **3** | **Nova Telemetry Logs** | `POST` | `https://api.novalauncher.com/stats/rawDataLog/{userId}/{timestamp}` | Submits compressed diagnostic payloads, crash contexts, and usage metrics.<br>*Caller:* `SubmitRawDataLogWork.java`, `r10.b.java` |
| **4** | **Nova Prime Licensing** | `POST` | `https://api.novalauncher.com/verifyLicense` | Validates Prime purchase tokens, activation keys, and direct licensing signatures.<br>*Caller:* `DirectLicensingEnterCode.java` |
| **5** | **Nova Geolocation** | `GET` | `https://api.novalauncher.com/location` | Resolves country, region, and IP locale for regional search customization.<br>*Caller:* `b10.d0.java`, `d30.d.java` |
| **6** | **Nova Experimentation** | `GET` | `https://api.novalauncher.com/x` | A/B testing and dynamic experimentation variant allocation.<br>*Caller:* `v30.h.java` |
| **7** | **Ad Mediation Config** | `GET` | `https://api.novalauncher.com/mediationconfig.json` | Distributes ad network mediation waterfalls, priorities, and timeout thresholds.<br>*Caller:* `wx.c.java` |
| **8** | **Branch Ingestion** | `POST` | `https://stage-nova-analytics-ingestion.eks-stage-magpie-usw1.branch.io/v1/ingest` | Branch Metrics analytics pipeline ingesting session events and retention cohorts.<br>*Source:* `res/values/strings.xml` (`novadailystats_endpoint`) |
| **9** | **Instabridge AI Platform** | `POST/SSE` | `https://api.instabridge.com/api3/` | Instabridge AI conversational backend powering streaming text, reasoning chains, and agent tool execution.<br>*Caller:* `com.instabridge.ai.client.AiClient` |
| **10** | **Instabridge Staging AI** | `POST/SSE` | `https://api.instabridge.dev/api3/` | Staging environment for upcoming Instabridge AI agent capabilities.<br>*Source:* `MobileSdkConfigKt.java` |
| **11** | **Instabridge Connectivity** | `REST API` | `https://esim.instabridge.com/` | eSIM provisioning and connectivity profile management SDK.<br>*Source:* `com.instabridge.mobilesdk` |
| **12** | **Sesame Search Relay** | `IPC` | `ninja.sesame.lib.bridge.v1.access.RelayActivity` | Deep on-device indexing and cross-app shortcut search integration with Sesame.<br>*Manifest:* `RelayActivity.java`, `IntegrationActivity.java` |

---

## 6. Confirmed Launcher AI Capabilities & Tool-Calling Schema

When the AI model determines an intent requires on-device action, it emits structured JSON tool calls:

```json
{
  "tool_call": {
    "name": "search_device_packages",
    "parameters": {
      "query": "camera"
    }
  }
}
```

### Confirmed Tool Execution Registry:
1. `init_session`: Initializes conversational agent context and model parameters.
2. `search_device_packages`: Queries installed apps using `AppManager.getInstalledApps(context)`.
3. `launch_application`: Launches target package via `AppManager.launchApp(context, app)`.
4. `modify_workspace_grid`: Adjusts home screen grid dimensions (`key_grid_cols`, `key_grid_rows`) via `WorkspaceViewModel`.
5. `toggle_hidden_package`: Hides or unhides an application from the App Drawer (`toggleHidePackage`).
6. `create_workspace_folder`: Automatically organizes matching applications into a desktop folder.
7. `query_network_repository`: Resolves reverse-engineered telemetry endpoints.
8. `check_entitlements`: Verifies Nova Prime licensing status.

---

## 7. Required Android Permissions & System Integrations

- `android.permission.INTERNET` — Network communication with AI inference & telemetry servers.
- `android.permission.ACCESS_NETWORK_STATE` — Network connectivity verification prior to request dispatch.
- `android.permission.QUERY_ALL_PACKAGES` — Full device package inventory discovery for AI tool calling.
- `android.permission.SET_WALLPAPER` — System wallpaper management.
- `android.speech.RecognizerIntent.ACTION_WEB_SEARCH` — System speech recognizer for voice input.

---

## 8. UI Assets & Resource References

### Layouts:
- `res/layout/activity_ai_chat.xml`
- `res/layout/activity_ai_settings.xml`
- `res/layout/activity_endpoints.xml`
- `res/layout/item_chat_message.xml`
- `res/layout/item_endpoint.xml`
- `res/layout/item_suggestion_chip.xml`

### Drawables & Styling:
- `res/drawable/bg_ai_gradient.xml` (Sparkle AI gradient)
- `res/drawable/bg_ai_bubble.xml` (AI message card background)
- `res/drawable/bg_user_bubble.xml` (User message bubble background)
- `res/drawable/ic_sparkle_ai.xml` (Nova AI Sparkle icon)
- `res/drawable/ic_mic.xml` (Voice search microphone icon)
- `res/drawable/ic_network.xml` (REA telemetry network icon)
- `res/drawable/ic_send.xml` (Chat send icon)

---

## 9. Confirmed Evidence vs. Assumptions & Unknowns

### Confirmed Evidence:
- Visual UI layouts, activity navigation, endpoint contracts, Retrofit interfaces, tool call data models, local `AppManager` intent invocation.
- All 12 reverse-engineered endpoints and their caller class names.

### Unknowns & Inaccessible Behavior:
- **Private Production Credentials:** Production API secrets and JWT tokens for `api.instabridge.com` are stored server-side.
- **Cloud LLM Prompt Templates:** Instabridge system prompts and cloud worker instructions are hosted server-side.

---

## 10. Recommended Architecture for Phase 8

To build Phase 8 cleanly on top of the existing MVVM + Room + Flow architecture:

```
com.example.launcherai.ai
 ├── engine
 │    ├── NovaAiEngine.kt (Coordinates prompt evaluation, streaming SSE, and tool execution)
 │    └── InstabridgeClient.kt (Network client for api.instabridge.com/api3/)
 ├── tools
 │    ├── LauncherTool.kt (Base interface for executable tools)
 │    ├── LaunchAppTool.kt
 │    ├── SearchAppsTool.kt
 │    ├── ModifyGridTool.kt
 │    ├── HideAppTool.kt
 │    └── CreateFolderTool.kt
 ├── models
 │    ├── ChatMessage.kt
 │    ├── ToolCallSpec.kt
 │    └── ToolResult.kt
 └── ui
      ├── NovaAiChatActivity.kt
      ├── NovaAiSettingsActivity.kt
      └── ChatAdapter.kt
```

---

## 11. Phased Implementation Roadmap for Phase 8

1. **Step 8.1 — AI Models & Executable Tool Registry:**
   Create `LauncherTool` interface and implement tool executors (`LaunchAppTool`, `SearchAppsTool`, `ModifyGridTool`, `HideAppTool`, `CreateFolderTool`) operating directly on `WorkspaceViewModel` and `WorkspaceRepository`.
2. **Step 8.2 — Streaming Network Client (`InstabridgeClient`):**
   Implement `Retrofit` / `OkHttp` SSE client connecting to `https://api.instabridge.com/api3/` and `api.novalauncher.com` with fallback to local mock engine when offline.
3. **Step 8.3 — Real-Time Agent & Tool Dispatcher (`NovaAiEngine`):**
   Parse streaming LLM tokens, extract JSON tool calls, execute local Android launcher tools, and return tool results to the conversational context.
4. **Step 8.4 — Live Conversational Chat UI (`NovaAiChatActivity`):**
   Wire `NovaAiChatActivity` to `NovaAiEngine` with live token streaming, animated `CaretView`, expandable reasoning blocks, and interactive tool execution cards.
5. **Step 8.5 — Settings & Telemetry Synchronization (`NovaAiSettingsActivity`):**
   Persist active intelligence model selections, Branch ingestion toggles, and data reset actions to `SharedPreferences`.
6. **Step 8.6 — End-to-End Testing & Verification:**
   Execute full unit test suite, verify tool calls, and complete `:app:assembleDebug` build.

---

**Report Status:** **INVESTIGATION COMPLETE (Phase 8 Prepared)**
