# Nova Reference vs LauncherAI Comparison

**Scope:** Source/resource audit of the supplied Nova Launcher v8.9.6 (build 89600) decompiled tree and the supplied LauncherAI project. Audit only: no application source, dependency, manifest, layout, resource, or build file was changed. Phase 8D was not started.

## Evidence and status convention

- **Evidence types:** direct source/resource inspection; DEX-derived; runtime observation; inference.
- **Confidence:** confirmed means directly visible in inspected source/resources; partial means the path or contract is only partly visible; unknown means the supplied material does not establish it.
- **Feature status:** MATCHED, PARTIAL, MISSING, DIFFERENT, or UNKNOWN, as defined in the request. Similar names alone do not establish a match.
- No APK/DEX for the reference and no screenshots were supplied. The reference tree is decompiled Java plus resources. Findings below do not claim runtime behavior unless explicitly identified as observed.
- REA CLI 6.9.0 was available. Its Android/JADX preflight (`rea doctor --provider jadx --json`) reported that Windows Application Control blocked REA's verified native controls. No REA Android Evidence, DEX trace, or runtime observation was produced. REA configuration was not changed. This report therefore cites direct inspection of the supplied tree, not REA-generated Evidence.

## 1. Executive Summary

The projects implement different AI products and end-to-end architectures. Nova's inspected chat is a native Android client for a server-managed AI service: it requests server-selectable models, sends authenticated conversation/run requests, consumes SSE-encoded AGUI events, dispatches native tools, and continues the model run with tool results. LauncherAI instead sends each prompt directly to OpenAI, Anthropic, or Google (or a local rule interpreter), persists messages in Room, and invokes a local registry of five launcher tools.

LauncherAI has useful component-level analogues—native chat, provider streaming parsers, tool cards, confirmation buttons, reasoning text, and local history—but they do not form an equivalent end-to-end flow. In particular, LauncherAI sends only the current prompt, does not return tool results to the provider, does not persist tool parameters, and does not use its provider registry in the main settings/chat path. Its local LLM provider is a placeholder, not an inference runtime.

| Area | Status | Principal evidence |
|---|---|---|
| Main AI chat architecture | DIFFERENT | Nova SDK chat view and server-run client vs LauncherAI activity and direct provider adapters |
| Model/provider selection | DIFFERENT | Nova's API-provided selectable models vs LauncherAI's local fixed index and labels |
| Streaming | PARTIAL | Both parse streaming responses, but event protocols and run orchestration differ |
| Tool-call round trip | MISSING | LauncherAI executes a tool but never submits its result for a follow-up model response |
| Native tools | PARTIAL | Five LauncherAI tools vs 27 confirmed core Nova tool names, plus dynamic tool families |
| HITL | PARTIAL | Both have approval UI, but LauncherAI loses params across persistence and does not resume the provider turn |
| Conversation history | DIFFERENT | Server conversation/thread API vs one local Room message stream |
| Local/on-device AI | DIFFERENT | LauncherAI rule-based local agent and non-inference stub; Nova's inspected chat is server-backed |
| Chat/settings UI | DIFFERENT | Both are native Android Views, but the inspected screens and interactions are not equivalent |
| Exact pixel match | UNKNOWN | Nova layout XML/screenshots are unavailable in the supplied tree; no device screenshots were provided |

No end-to-end feature is classified MATCHED. Narrow similarities such as “both have a send button” or “both can search installed apps” are not equivalent behavior.

## 2. Reference Nova AI Architecture

### Rendering technology and entry path

The inspected entry is `com.instabridge.ai.chat.ui.AiChatActivity`. It creates the V1 `AiChatView`, which inflates native Android Views and wires a `RecyclerView`, `ChatAdapter`, `ChatInputBar`, suggestion rows, loading skeleton, retry banner, scroll-to-latest control, and tool-result callback. The AI chat screen is therefore native Views, not Compose or a WebView UI.

`AiChatViewV2` and V2 view holders are also present in the decompiled tree. They include a richer message renderer, streaming caret, follow-up/source elements, and tool-card renderers. The inspected `AiChatActivity` path instantiates V1, not V2; V2 features must not be attributed to the active chat screen without additional launch-path evidence.

### Send, stream, tools, and continuation

The observed source flow is:

1. `ChatInputBar` invokes the send callback in `AiChatView`.
2. `AiChatView` creates/uses a conversation thread and calls `AiClient.runWithHitl` (with the request data and tool definitions).
3. `AiClient` POSTs to the configured service base plus `api/chat/run`, adds the SDK's bearer authorization and mobile/app request headers, and consumes an SSE response.
4. SSE `data:` payloads are decoded into AGUI event classes, including text/run lifecycle, reasoning, tool-call start/arguments/end, snapshot, error, and service/quota state.
5. `AiChatView` updates `ChatItem`s and the V1 adapter. Tool-call events are routed to inline/chat tool handling or native `AiToolsManager` execution; confirmation-required calls are represented as cards and return a user decision/result.
6. `AiClient` sends tool results back with the original thread/run context using its continuation methods; `runWithHitl` coordinates repeated turns and bounded tool execution rather than treating a tool call as the final assistant response.

`executeWithAuthRetry` retries a request after HTTP 401 by notifying the auth layer, asking the token provider again, replacing the bearer token, and resending. `AiChatView` also wires retry/regenerate callbacks. This is not evidence of arbitrary retry for all transport or server failures.

### Model selection and conversation storage

`ModelSelectorSheet` calls `ConversationApiClient.getSelectableModels()` and renders an Auto row plus the server-provided models. It displays descriptions and PRO markers, gates unavailable PRO-only choices, stores the selected model preference, and offers an upgrade path. Exact upstream model IDs/providers are not established by this client source: they come from the server response and are not hardcoded in the inspected model selector.

`ConversationApiClient` includes selectable-model and conversation list/create/get/delete APIs. The chat is associated with a service-side thread/conversation; this differs from LauncherAI's single local Room history. The supplied source does not establish the current upstream model inventory or its present availability.

### Tools and JavaScript bridge

`AiToolsManager` converts tool definitions to API format and registers them at the configured base plus `api/tools/register`, using a bearer token. `AiTool` defines tool metadata, availability checks, execution, optional user-initiated behavior, and permission helpers. Its core tool inventory is listed in §6. The code also imports/uses dynamic per-app and BII capability families; only names confirmed in inspected source are listed below.

`AiWebBridge` declares a JavaScript interface named `NativeAI` with channel/action dispatch and an async responder; `ToolsChannelHandler` supports `list`, `available`, and `execute`. This bridge is separate from the native AI chat renderer. A search of the supplied Java tree found no `new AiWebBridge(...)`/`addJavascriptInterface(..., "NativeAI")` attachment site. The launcher has an unrelated `NativeAndroid` bridge in `NovaMobileActivity`; it is not proof that `NativeAI` is attached. Consequently, bridge implementation is confirmed, but its actual consumer/attachment path is unknown.

## 3. LauncherAI AI Architecture

The inspected path is:

1. `NovaAiChatActivity` accepts typed text or a suggestion and calls `NovaAiEngine.submitPrompt`.
2. `NovaAiEngine` saves a user `ChatMessageEntity`, then calls `activeProvider.streamPrompt(prompt)`. It passes only the current prompt—not conversation history, a conversation/thread ID, or prior tool results.
3. Provider adapters issue direct HTTPS calls. They emit `Token`, `Reasoning`, `ToolCall`, `Error`, and `Complete` events.
4. For a tool call, `NovaAiEngine` invokes `HitlConfirmationManager.requiresConfirmation`. Non-confirmed tools execute immediately through `AiToolBridge`; confirmed tools become a text prompt/card.
5. The engine saves and emits assistant entities while processing events. It does not call the provider again with a tool result.
6. `NovaAiChatActivity` observes the Room history and maps rows to `ChatMessage`. On confirm it reads `chatMsg.toolCallParams`, but that field is not represented in `ChatMessageEntity` or populated by the entity-to-message mapping; the callback therefore falls back to an empty map. It calls `confirmAndExecuteTool`, which saves the updated result but also does not continue the AI turn.

`AiProviderRegistry` defines provider IDs and instances, but the inspected settings path uses a stored integer selection and directly assigns provider instances to `NovaAiEngine.activeProvider`. The registry is not the active selection mechanism. The five schemas are repeated in each remote provider rather than generated from `AiToolBridge`.

The settings flow also has a misleading fallback: for a remote provider with no key, the status text says it is falling back to the local agent, but `updateStatusUI()` still assigns the selected remote provider with an empty key. That provider emits a configuration error; it does not switch to `LocalAgentProvider`.

## 4. Provider and Model Comparison

### Reference service vs LauncherAI direct providers

| Provider/service | Reference evidence | LauncherAI model IDs in source | Endpoint and authentication | Request/stream/tool behavior |
|---|---|---|---|---|
| Nova AI service | `AiClient` and `ConversationApiClient` use a configured service base. Reference upstream vendor/provider and model IDs are **UNKNOWN**; selectable models are returned by `api/models/selectable`. | Not applicable | `api/chat/run`; SDK bearer token plus mobile/app headers. Token values and private service configuration are intentionally not reproduced here. | Server-run request includes conversation/run context and tool definitions; SSE `data:` JSON is translated to AGUI events; tool results are submitted on continuation. |
| OpenAI | No direct OpenAI endpoint/model mapping established in inspected Nova client. | `gpt-4o`, `gpt-4o-mini`, `o1`, `o3-mini`, `gpt-4.5-preview` | `https://api.openai.com/v1/chat/completions`; `Authorization: Bearer` API key. | Chat Completions JSON, `stream: true`, one current user message plus five function tools; parses `choices[0].delta`. Parser inspects only the first streamed tool call and does not robustly assemble parallel/multi-chunk tool calls. |
| Anthropic | No direct Anthropic endpoint/model mapping established in inspected Nova client. | `claude-3-7-sonnet-latest`, `claude-3-5-sonnet-latest`, `claude-3-5-haiku-latest`, `claude-3-opus-latest`; snapshots `claude-3-7-sonnet-20250219`, `claude-3-5-sonnet-20241022`, `claude-3-5-haiku-20241022`, `claude-3-opus-20240229` | `https://api.anthropic.com/v1/messages`; `x-api-key` and `anthropic-version: 2023-06-01`. | Messages API with `max_tokens: 1024`, `stream: true`, five `tools`/`input_schema`; parses event/data lines and buffers partial `input_json_delta`, but the content-block/tool lifecycle and fallback parser are incomplete for robust mixed/multiple calls. |
| Google Gemini | No direct Gemini endpoint/model mapping established in inspected Nova client. | `gemini-2.5-flash`, `gemini-2.5-pro`, `gemini-2.0-flash`, `gemini-1.5-flash`, `gemini-1.5-pro`, `gemini-1.5-flash-8b`; preview `gemini-2.0-flash-lite-preview-02-05` | `https://generativelanguage.googleapis.com/v1beta/models/{model}:streamGenerateContent?alt=sse`; `x-goog-api-key`. | Gemini `contents`/`functionDeclarations`, parses candidate parts and function calls. A model-list request exists, but failures/empty results silently fall back to the local static list. |
| Local agent / on-device | Reference chat flow inspected is service-backed. A matching local LLM/provider implementation is **UNKNOWN** from the supplied source. | Rule-based `LocalAgentProvider`; `OnDeviceLlmProvider` advertises LiteRT/Gemma but has no inference call. | Local rule agent makes no network request. Remote providers call official vendor endpoints directly from the app. | Rule agent maps keywords to canned tool arguments/text. On-device stub returns a success-shaped text only if a model path is set; it does not load or run weights. |

The LauncherAI model lists are source declarations, not live API verification. Do **not** infer that any listed model ID is still accepted or available today. The supplied reference source provides no upstream IDs to compare. For modernization, retrieve the current vendor catalogs/retirement policies at implementation time and use an explicit compatibility check; do not copy private Nova service credentials or assume its backend is accessible.

## 5. Streaming Protocol Comparison

| Aspect | Reference Nova | LauncherAI |
|---|---|---|
| Wire format | SSE lines with JSON payloads decoded into AGUI event types. | Provider-specific SSE parsing: OpenAI/Gemini primarily `data:`; Anthropic parses `event:` and `data:`. |
| Text | AGUI text-message deltas are associated with run/message state. | Provider delta text becomes `AiEvent.Token`; engine concatenates into one string. |
| Reasoning | Reasoning start/content/end events exist in the protocol. V1 UI shows thinking/status/shimmer activity; a collapsible raw reasoning transcript in the active V1 path was not confirmed. | `AiEvent.Reasoning` stores one text value on the message; adapter has a collapsible reasoning box. Provider adapters mostly emit “connecting” status rather than model reasoning tokens. |
| Tool stream | Tool-call start/args/end events preserve run/call context and support continuation. | Tool call is parsed into one name/params event; OpenAI only reads index zero and Claude's stream assembly is fragile. |
| Tool results | Submitted back to the server run; additional assistant events are consumed. | Executed locally, displayed/stored, but never sent back to the provider. |
| Completion/errors | Explicit run lifecycle/error events and UI retry/regenerate hooks. | `Complete` is emitted in `finally`; engine has no dedicated completion state. HTTP statuses map to text errors; no automatic retry observed in provider adapters. |

Classification: **PARTIAL** for streaming text; **DIFFERENT** in protocol/state handling; **MISSING** for the full tool-result continuation behavior.

## 6. Native Tool Comparison

### Confirmed reference core inventory

The inspected `AiTool` source/metadata confirms these 28 core tool names. This is not a claim that every dynamic catalog was enumerated:

`add_contact`, `compose_email`, `create_calendar_event`, `make_call`, `navigate_to`, `open_app`, `open_calculator`, `open_calendar`, `open_contact`, `open_display_settings`, `open_location_settings`, `open_maps`, `open_messages`, `open_general_settings`, `open_sound_settings`, `read_calendar_events`, `read_notifications`, `search_contacts`, `search_device`, `search_nearby`, `search_play_store`, `search_web`, `send_sms`, `set_alarm`, `set_timer`, `share_text`, `view_calendar_event`, `view_contacts`.

The source also references communication, system, media, standard-intent, per-app, and BII tool families. Additional concrete names confirmed in the inspected source include `send_message`, `open_url`, `read_call_log`, `read_sms`, `set_volume`, `toggle_flashlight`, `media_now_playing`, `media_play_pause`, `media_previous_track`, and `media_skip_track`. Dynamically assembled per-app/BII names are not exhaustively enumerable from this review and are not invented here.

Nova tool definitions include names, display names, descriptions, parameter maps, tool types, confirmation metadata, availability checks, and execution. `AiTool` contains permission helpers and imports contacts/calendar/location/notification APIs. Per-tool schemas and runtime permission outcomes for the full catalog were not exhaustively extracted; where not proven, they remain unknown.

### LauncherAI five tools

| LauncherAI tool and schema | Validation/execution/result | Confirmation and permission | Reference comparison |
|---|---|---|---|
| `search_device_packages(query: string, required)` | Case-insensitive label/package substring search over installed apps; returns matching label/package lines or “no matching applications.” Blank query returns all apps. | No HITL. Uses installed-app visibility; manifest requests `QUERY_ALL_PACKAGES`. | **PARTIAL** analogue to Nova `search_device`; different tool name and contract. |
| `launch_application(packageName: string, required)` | Exact package match in installed apps; launches it or returns missing/not-installed error. | No HITL. No runtime confirmation or permission gate in this implementation. | **PARTIAL** analogue to Nova `open_app`; not the same name/schema/launch flow. |
| `modify_workspace_grid(cols: integer, rows: integer, both required)` | Parses numeric/string values; defaults invalid/missing values to 4x5, clamps columns 3–6 and rows 3–7, writes shared preferences, returns resulting grid. | HITL required by tool flag; confirmation prompt echoes values. No runtime permission check. | No equivalent among the 27 confirmed core names; **DIFFERENT** LauncherAI-only behavior. |
| `toggle_hidden_package(packageName: string, required)` | Requires exact installed package, toggles it in `key_hidden_packages`, reports hidden/restored state. | HITL required; no runtime permission check. | No equivalent among the 27 confirmed core names; **DIFFERENT**. |
| `create_workspace_folder(title: string, packages: string[], both required)` | Requires at least two package names and at least two installed matches; creates a folder from the first two, adds remaining installed apps, returns folder/title/app list. Invalid/missing title defaults to “AI Folder”; extra invalid names are dropped. | HITL required; no runtime permission check. | No equivalent among the 27 confirmed core names; **DIFFERENT**. |

The five provider schemas are repeated verbatim across OpenAI, Claude, and Gemini adapters. They are not generated from `AiToolBridge` or validated against its registry, so drift is possible. `ToolResult` is converted to local display text only. **No LauncherAI tool result is serialized into a provider-specific follow-up request.**

## 7. HITL Comparison

**Reference — PARTIAL-to-confirmed flow:** Reference tool metadata includes `requiresConfirmation`; `AiChatView` binds tool-result callbacks, renders tool/confirmation states, and passes outcomes into `AiClient.runWithHitl`/continuation. This is integrated with thread/run state. Exact per-tool confirmation policy was not exhaustively mapped.

**LauncherAI — PARTIAL:** `modify_workspace_grid`, `toggle_hidden_package`, and `create_workspace_folder` require confirmation. Search and launch do not. The card offers Confirm and Cancel. However, the tool params do not survive the Room entity/chat mapping; confirmation falls back to `{}`. Missing values may then trigger unsafe/default behaviors (for example grid silently defaults to 4x5). Confirming updates local history but does not resume the provider run. Cancel does not send a rejection/result to the provider.

Required parity work: persist the complete tool call ID/name/arguments/status, render a truthful preview, validate again at execution time, preserve cancellation/denial, and send tool result or rejection back to the same run before allowing the assistant turn to finish.

## 8. Chat UI Comparison

### Screen/component comparison

| UI element | Reference Nova | LauncherAI | Difference/status |
|---|---|---|---|
| Chat toolbar | V1 Activity/SDK view with navigation/menu actions including conversation/history-related actions; model chip/status is managed by `AiChatView`. Exact active XML geometry unavailable. | Native 56dp bar: back, “Nova AI”/“PRO”, status label, clear-chat and settings buttons; 16dp horizontal padding. | **DIFFERENT** interaction and hierarchy. |
| User/assistant messages | Native `RecyclerView`/`ChatAdapter`; markdown/rich content, sources/citations and feedback/copy/regenerate hooks are present in inspected SDK code. | One row layout; user and assistant bubbles with timestamp; assistant copy only; plain `TextView` content. | **DIFFERENT**; source parity is not visual parity. |
| Message geometry | Reference resource `ai_sdk_chat_input_min_height=60dp`; layout XML not found in inspected reference resource tree, so bubble widths/padding/radii are unknown. | Row padding 12dp horizontal/6dp vertical; user max width 300dp and 12dp padding; assistant max width 320dp and 14dp padding; text 14sp, timestamps 10sp. | Exact pixel comparison **UNKNOWN**; LauncherAI values confirmed from XML. |
| Colors | Reference resources include user bubble `#ff2e3b43`, composer `#ff1c242b`, brand orange via theme resource, muted `#ff9ca3af`. V2-specific palette exists but is not assumed to be active V1. | Dark background `#0F1117`, surface `#1A1D27`; user bubble is an accent gradient with 18dp left/4dp top-right/18dp bottom-left/18dp bottom-right corners; assistant bubble uses local drawable/theme. | **DIFFERENT** palette and bubble treatment; reference exact active V1 drawable/radius unknown. |
| Reasoning | V1 has thinking/status/shimmer text; no confirmed collapsible raw reasoning block on active path. V2 class has additional rendering code but is not directly used by inspected Activity. | Collapsible “Reasoning Process” box in the assistant bubble, 11sp text, chevron; adapter stores an expanded flag in the holder. | **DIFFERENT**. The reference active path does not establish an equivalent user-expandable reasoning transcript. |
| Streaming/caret | V1 chat updates on stream and has skeleton/loading/retry UI; V2 has explicit caret behavior, but active V1 use is unconfirmed for that feature. | Engine accumulates token text and Room emits updates; no animated caret or explicit streaming state in inspected adapter. | **PARTIAL** text updates; animation/interaction **DIFFERENT**. |
| Tool cards | Typed tool-call cards and tool-result callbacks; V2 includes dedicated card holder. | Inline tool name/result card; `⚡ Tool Executed` label, result text, 11sp. | **PARTIAL** concept, **DIFFERENT** design and lifecycle. |
| Confirmation cards | Reference tool-call/confirmation states are integrated with tool result/run continuation. Exact V1 geometry not recoverable. | Card with warning title, prompt, Cancel and Confirm & Execute; uses a 16dp-round card background from the local resource. | **PARTIAL** interaction; parameter persistence/continuation is incomplete. |
| Composer/input | `ChatInputBar`, model chip, send, 60dp minimum-height resource, suggestions and keyboard/inset handling. | EditText in bottom surface, 8dp container padding, 14sp input, 10dp vertical input padding, max 4 lines; 44dp send image button; fixed suggestion chips above. | **DIFFERENT** model selection is not part of the composer; exact reference dimensions unknown. |
| Model selector | API-loaded Material bottom sheet with Auto, descriptions, selection, PRO gating/upgrade, loading/error states. | `AlertDialog` single-choice list of nine hardcoded labels stored by integer index. | **DIFFERENT**. |
| Settings | SDK/account/profile/consent/billing and server model controls; no inspected UI for end-user arbitrary vendor API keys. | Separate long scroll screen for fixed model list, local API keys, telemetry labels/toggles, clear-data, endpoint inspector. Several switches only show Toasts. | **DIFFERENT**; LauncherAI includes unrelated demo/debug controls. |
| Loading/error/empty | Reference skeleton, network retry banner, error events, conversation/suggestion states. | No explicit streaming/loading/error row; provider errors become assistant text. Empty DB inserts a synthetic welcome message. | **DIFFERENT**. |
| History/navigation | Conversation API, history/list actions and per-conversation lifecycle. | One Room history observed in chat; clear button deletes it. No inspected conversation picker or per-thread switching. | **DIFFERENT**. |

### Reference screen inventory and classification

| Screen/component | Status | Evidence/limit |
|---|---|---|
| Native AI chat V1 | PARTIAL | Directly used by `AiChatActivity`; no matching screenshot or source layout XML. |
| Chat V2 renderer | UNKNOWN as active screen | Class exists; active Activity instantiates V1. |
| Model-selection bottom sheet | DIFFERENT | Server-fed Auto/PRO model sheet vs local alert dialog. |
| Conversation history/list | PARTIAL | Reference API/list behavior inspected; exact visual screen resources unavailable. LauncherAI has no comparable thread picker. |
| Tool execution and confirmation cards | PARTIAL | Both exist, but different content/interaction and LauncherAI lacks continuation. |
| AI settings/API configuration | DIFFERENT | Reference account/service configuration vs local vendor-key/demo settings. |
| Welcome/empty screen | DIFFERENT | Reference suggestions/loading state vs LauncherAI's persisted synthetic welcome message. |
| WebView AI screen | UNKNOWN/not established | `NativeAI` bridge class exists; no chat WebView/bridge attachment path found. |
| Pixel-level match | UNKNOWN | No reference screenshots; reference layouts/drawables/metrics were not available in inspected resources. |

No screenshots were supplied, and no emulator/device capture was made. Recoverable values above come from source XML/resource files only. Do not treat colors named `ai_sdk_v2_*` or V2 view-holder code as V1 active-screen evidence.

## 9. Settings and Model Selector Comparison

Reference selection is dynamic and service-authoritative. The selector loads `SelectableModelsResponse`, offers Auto, uses model IDs/display names/descriptions and plan availability, persists a model preference, and directs users toward an upgrade when appropriate.

LauncherAI stores a model index in `SharedPreferences`; the nine settings choices are hardcoded display labels. There is no provider-neutral model list in the settings flow, and `AiProviderRegistry` is bypassed. The settings file allows a user-entered vendor API key and displays its last four characters. Several switches (AI enablement, Branch ingestion, raw logging) only display Toasts in the inspected listener; they do not toggle the corresponding runtime feature.

Status: model selection **DIFFERENT**; provider management **PARTIAL**; reference vendor/model mapping **UNKNOWN**.

## 10. Local AI and On-Device AI Comparison

`LocalAgentProvider` is deterministic keyword matching. It synthesizes tool arguments such as fixed grid values and example folder package names; it does not generate language-model responses. `OnDeviceLlmProvider` has no LiteRT/MediaPipe runtime call, model download, tokenizer, or inference session. If configured with a path, it emits a fixed “initialized” token rather than running inference; if not, it reports missing weights.

The inspected Nova chat uses a remote service and server-provided model selection. Although its native tools execute on device, that is not equivalent to a local language model. No evidence in the supplied Nova sources establishes an on-device LLM. LauncherAI local AI is therefore **DIFFERENT**, and parity for actual offline LLM inference is **MISSING/UNKNOWN** on the reference side.

## 11. API and Security Comparison

| Topic | Reference Nova | LauncherAI |
|---|---|---|
| Credential boundary | SDK service bearer token, refreshed on HTTP 401; token values/private service configuration intentionally omitted. | User-provided vendor API keys are stored in app preferences through `SecureKeyStorage` and sent directly to vendor endpoints in headers. |
| Key protection | Service token handling is delegated to SDK/auth provider in inspected code; underlying secure storage is not fully established here. | `SecureKeyStorage` uses Base64/XOR obfuscation, not cryptographic encryption. Source itself warns this is not production-safe. |
| Network destinations | Configured Nova service endpoints, including chat/model/conversation/tool-registration routes. Do not infer direct upstream vendors from this. | Official OpenAI, Anthropic, and Gemini HTTPS endpoints. |
| Tool authority | SDK tool availability/permission helpers plus per-tool native execution; reference user-initiated/confirmation paths exist. Full tool-by-tool permission matrix not inspected. | Five local tools; package visibility via `QUERY_ALL_PACKAGES`; confirmations on only three state-changing tools; launch executes without confirmation. |
| Network security | HTTPS service URLs in inspected client. Certificate pinning, network security config, and complete transport policy are **UNKNOWN**. | HTTPS vendor URLs in adapters. No reviewed network-security configuration was found; pinning and other transport policy are **UNKNOWN**. |
| Data retention | Server conversation APIs and account-associated lifecycle. Exact retention policy is outside the supplied client evidence. | Room-backed local chat; the inspected message schema contains no conversation ID, provider/model, tool-call arguments, or tool-call ID. |
| Error/data exposure | SDK errors and auth handling. No secrets reproduced in this report. | Provider errors are converted to display text; adapters attempt API-key redaction in exception messages. Settings shows last four characters. |

Security status: key storage **DIFFERENT/INCOMPLETE**; reference/backend credentials **not copied**; exact reference retention and transport hardening **UNKNOWN**.

## 12. Missing Features

1. A provider-neutral request/conversation abstraction that submits conversation history and provider/tool results.
2. Complete tool-call lifecycle: assemble all streamed calls/arguments, execute safely, serialize results, and continue the same provider turn.
3. Persisted tool-call arguments/IDs/status and correct confirmation/cancel recovery after process recreation.
4. Reference-style API-fed model selection, Auto semantics, availability and entitlement gating.
5. A working `AiProviderRegistry` selection path shared by settings and execution.
6. A true on-device inference implementation or honest removal/disablement of the current success-shaped stub.
7. Streaming lifecycle UI: explicit running/completion/error/cancel state, retry/regenerate handling, and a caret if parity requires it.
8. Conversation/thread IDs, conversation list/switch/delete semantics, and context management.
9. Reference-equivalent citation/source rendering, feedback, rich markdown, follow-ups, and action surfaces where supported by the active V1 reference screen.
10. Truthful settings switches and secure backend-mediated secret handling rather than shipping vendor keys in an Android client.
11. A complete visual specification derived from reference screenshots/layouts; exact pixel parity cannot be implemented from the currently supplied evidence.

## 13. Incorrect or Incomplete Implementations

- The settings “fallback to local” message is inconsistent with provider assignment: a remote provider with a blank key remains active and emits an error.
- The engine omits history from every provider request and does not reuse thread/run context.
- The model-facing tool schemas are duplicated in each provider and can diverge from native tool definitions.
- OpenAI only reads the first `tool_calls` element in a delta; it does not assemble streamed argument fragments/calls into a complete request-scoped call.
- Claude has a partial-JSON buffer, but tool-call emission around block start/stop and fallback parsing is not robust enough to establish correct handling of all valid multi-block/multi-tool streams.
- Tool results never return to any provider; a tool call may be shown as an assistant answer without model synthesis of the result.
- HITL arguments are lost in Room/entity mapping; confirm can execute with an empty parameter map and local tool defaults.
- `launch_application` is marked non-confirmation despite performing an external action. Whether this is an intended policy requires product approval; it is not equivalent to Nova's per-tool confirmation policy.
- `OnDeviceLlmProvider` reports “initialized” without invoking inference when a model path is present.
- Model catalog declarations are not checked against live provider availability. Gemini's catalog fetch silently falls back to static IDs on errors, so a stale catalog may appear valid.
- Room history is one global message stream with no conversation/provider/model/tool-call-parameter fields.
- The chat adapter's reasoning expansion state is held on the recycled view holder rather than per message; this can cause expanded/collapsed state to leak between recycled rows.
- AI enablement/telemetry settings listeners shown in the inspected activity only display status Toasts; they do not establish working feature toggles.

## 14. Confirmed Matches

No feature is confirmed equivalent end-to-end.

Confirmed **partial analogues only**:

- Both render AI conversations with native Android Views and RecyclerViews.
- Both can display streamed text and a reasoning/status field.
- Both have tool registries and native tool execution.
- Both show confirmation controls for some native actions.
- Both persist some conversation/message data (reference service-side, LauncherAI Room).
- LauncherAI package search and launch broadly resemble Nova `search_device` and `open_app`, but names, schemas, eligibility, and execution contracts differ.

These are component-level similarities, not MATCHED classifications.

## 15. Differences and Required Changes

| Priority | Difference | Required direction |
|---|---|---|
| P0 | Tool-call state is not round-tripped | Define a provider-neutral call/result model; aggregate stream deltas; execute; send result/rejection to the same provider with the full context. |
| P0 | Confirmation parameters disappear | Persist structured tool arguments and IDs; revalidate before execution; bind approval/cancel to the exact pending action. |
| P0 | Tool actions execute without consistent policy | Review each tool's side-effect and permission policy; require explicit confirmation where appropriate; do not rely on provider tool schemas as validation. |
| P1 | Provider selection bypasses registry and false fallback | Route settings through stable provider IDs/registry; make blank-key fallback actually select local or show a blocking configuration state. |
| P1 | No conversation context/history | Add thread/conversation IDs and explicit context retention/compaction; avoid sending unrelated chats. |
| P1 | Model catalog is hardcoded and stale-prone | Use provider-supported model discovery/compatibility metadata and explicit unavailable states; do not claim current availability from source snapshots. |
| P1 | API keys live in client preferences | Prefer a secure backend proxy; otherwise use platform-backed encryption and threat-model exposed client keys. |
| P2 | UI is a custom mock rather than reference flow | Rebuild only after obtaining active V1 reference layout/screenshots; match actual toolbar, composer, cards, loading and error interactions. |
| P2 | No verified local inference | Integrate a real supported runtime/model lifecycle or label the choice as unavailable; remove success-shaped placeholder behavior. |

## 16. Prioritized Implementation Roadmap

### Functional roadmap

1. **Repair tool round trip and HITL state (P0).** Add typed call IDs/arguments/results to persistence and events; send success, failure, and denial back to the same provider; ensure cancellation and retries cannot duplicate actions.
2. **Harden native tool policy (P0).** Validate schemas and parameters at execution, check package visibility/permissions, define per-tool confirmation policy, and produce structured results.
3. **Unify provider selection and errors (P1).** Use `AiProviderRegistry` from settings and chat; correct blank-key fallback; distinguish configuration, auth, quota, network, and model-unavailable errors.
4. **Add conversation context (P1).** Persist conversation IDs and message roles/tool messages; submit bounded context and implement clear/new/switch/delete behavior.
5. **Replace static model assumptions (P1).** Query current supported models or maintain a verified compatibility catalog with explicit dates; gracefully handle retired IDs without success-shaped fallback.
6. **Resolve secret and local-model claims (P1).** Move vendor calls behind a backend where feasible; do not claim LiteRT inference until an actual model loads and generates output.
7. **Match visual design from evidence (P2).** Obtain reference screenshots/layout resources or a reference runtime capture before tuning dp/color/radius values.

### Separate prioritized UI checklist

- **UI-P0:** Match reference chat hierarchy and toolbar actions; distinguish new chat, history, and delete rather than one clear-history action.
- **UI-P0:** Match composer/model chip, suggestions, keyboard/insets, loading, retry, and cancellation behavior.
- **UI-P1:** Match message types (rich text, source/citation, feedback, regenerate/copy) and streaming states.
- **UI-P1:** Match tool execution/confirmation cards, including parameter preview, pending/result/denied/failed states.
- **UI-P2:** Reproduce model bottom sheet, Auto selection, descriptions, availability/PRO gate and error/loading states.
- **UI-P2:** Remove settings/telemetry demo controls unless they are actual product functionality; reproduce verified reference account/settings entry points.
- **UI-P3:** Tune exact typography, margins, colors, radii, icons, and animations only after obtaining reliable reference screenshots or original layout resources.

This is an audit roadmap, not a request to begin implementation. **Phase 8D was not started.**

## 17. Evidence and Source File References

### Reference files inspected

**Build/version and core chat**

- `Nova Launcher/app/build.gradle` — version code 89600 and version name `89600 (8.9.6)`.
- `Nova Launcher/app/src/main/java/com/instabridge/ai/chat/ui/AiChatActivity.java`
- `Nova Launcher/app/src/main/java/com/instabridge/ai/chat/ui/AiChatView.java`
- `Nova Launcher/app/src/main/java/com/instabridge/ai/chat/ui/ChatAdapter.java`
- `Nova Launcher/app/src/main/java/com/instabridge/ai/chat/ui/ChatInputBar.java`
- `Nova Launcher/app/src/main/java/com/instabridge/ai/chat/ui/ModelSelectorSheet.java`
- `Nova Launcher/app/src/main/java/com/instabridge/ai/chat/ui/v2/AiChatViewV2.java`
- `Nova Launcher/app/src/main/java/com/instabridge/ai/chat/ui/v2/AiMessageViewHolderV2.java`
- `Nova Launcher/app/src/main/java/com/instabridge/ai/chat/ui/v2/ToolCallCardViewHolderV2.java`

**Service, models, tools, and bridge**

- `Nova Launcher/app/src/main/java/com/instabridge/ai/client/AiClient.java`
- `Nova Launcher/app/src/main/java/com/instabridge/ai/chat/data/ConversationApiClient.java`
- `Nova Launcher/app/src/main/java/com/instabridge/ai/chat/model/SelectableModelsResponse.java`
- `Nova Launcher/app/src/main/java/com/instabridge/ai/bridge/AiWebBridge.java`
- `Nova Launcher/app/src/main/java/com/instabridge/ai/bridge/ToolsChannelHandler.java`
- `Nova Launcher/app/src/main/java/com/instabridge/ai/tools/AiTool.java`
- `Nova Launcher/app/src/main/java/com/instabridge/ai/tools/AiToolsManager.java`
- `Nova Launcher/app/src/main/java/com/instabridge/ai/tools/OpenUrl.java`
- `Nova Launcher/app/src/main/java/com/instabridge/ai/tools/SendMessageTool.java`
- `Nova Launcher/app/src/main/java/com/instabridge/ai/tools/CommunicationTools.java`
- `Nova Launcher/app/src/main/java/com/instabridge/ai/tools/MediaTools.java`
- `Nova Launcher/app/src/main/java/com/instabridge/ai/tools/SystemTools.java`
- `Nova Launcher/app/src/main/java/com/instabridge/ai/tools/standard/StandardIntentsCatalog.java`
- `Nova Launcher/app/src/main/java/com/instabridge/ai/tools/standard/FileTools.java`
- `Nova Launcher/app/src/main/java/com/instabridge/ai/tools/standard/ClockExtensionsTools.java`
- `Nova Launcher/app/src/main/java/com/teslacoilsw/launcher/novamobile/NovaMobileActivity.java` — separate `NativeAndroid` bridge reference.

**Reference resources**

- `Nova Launcher/app/src/main/res/values/colors.xml`
- `Nova Launcher/app/src/main/res/values/dimens.xml`
- `Nova Launcher/app/src/main/res/values/strings.xml`
- No original AI chat layout XML was available in the inspected decompiled resource tree; view IDs and SDK resource IDs are present in the Java.

### LauncherAI files inspected

- `LauncherAI/app/src/main/AndroidManifest.xml`
- `LauncherAI/app/src/main/java/com/example/launcherai/NovaAiChatActivity.kt`
- `LauncherAI/app/src/main/java/com/example/launcherai/NovaAiSettingsActivity.kt`
- `LauncherAI/app/src/main/java/com/example/launcherai/ChatAdapter.kt`
- `LauncherAI/app/src/main/java/com/example/launcherai/ChatMessage.kt`
- `LauncherAI/app/src/main/java/com/example/launcherai/ai/execution/NovaAiEngine.kt`
- `LauncherAI/app/src/main/java/com/example/launcherai/ai/provider/AiProvider.kt`
- `LauncherAI/app/src/main/java/com/example/launcherai/ai/provider/AiEvent.kt`
- `LauncherAI/app/src/main/java/com/example/launcherai/ai/provider/AiProviderRegistry.kt`
- `LauncherAI/app/src/main/java/com/example/launcherai/ai/provider/OpenAiProvider.kt`
- `LauncherAI/app/src/main/java/com/example/launcherai/ai/provider/OpenAiModels.kt`
- `LauncherAI/app/src/main/java/com/example/launcherai/ai/provider/ClaudeAiProvider.kt`
- `LauncherAI/app/src/main/java/com/example/launcherai/ai/provider/ClaudeModels.kt`
- `LauncherAI/app/src/main/java/com/example/launcherai/ai/provider/GeminiAiProvider.kt`
- `LauncherAI/app/src/main/java/com/example/launcherai/ai/provider/GeminiModels.kt`
- `LauncherAI/app/src/main/java/com/example/launcherai/ai/provider/LocalAgentProvider.kt`
- `LauncherAI/app/src/main/java/com/example/launcherai/ai/provider/OnDeviceLlmProvider.kt`
- `LauncherAI/app/src/main/java/com/example/launcherai/ai/repository/AiRepository.kt`
- `LauncherAI/app/src/main/java/com/example/launcherai/ai/bridge/AiToolBridge.kt`
- `LauncherAI/app/src/main/java/com/example/launcherai/ai/confirmation/HitlConfirmationManager.kt`
- `LauncherAI/app/src/main/java/com/example/launcherai/ai/tools/LauncherTool.kt`
- `LauncherAI/app/src/main/java/com/example/launcherai/ai/tools/impl/SearchDevicePackagesTool.kt`
- `LauncherAI/app/src/main/java/com/example/launcherai/ai/tools/impl/LaunchApplicationTool.kt`
- `LauncherAI/app/src/main/java/com/example/launcherai/ai/tools/impl/ModifyWorkspaceGridTool.kt`
- `LauncherAI/app/src/main/java/com/example/launcherai/ai/tools/impl/ToggleHiddenPackageTool.kt`
- `LauncherAI/app/src/main/java/com/example/launcherai/ai/tools/impl/CreateWorkspaceFolderTool.kt`
- `LauncherAI/app/src/main/java/com/example/launcherai/ai/security/SecureKeyStorage.kt`
- `LauncherAI/app/src/main/java/com/example/launcherai/data/db/ChatMessageEntity.kt`
- `LauncherAI/app/src/main/java/com/example/launcherai/data/db/ChatDao.kt`
- `LauncherAI/app/src/main/java/com/example/launcherai/data/db/LauncherDatabase.kt`
- `LauncherAI/app/src/main/res/layout/activity_ai_chat.xml`
- `LauncherAI/app/src/main/res/layout/item_chat_message.xml`
- `LauncherAI/app/src/main/res/layout/activity_ai_settings.xml`
- `LauncherAI/app/src/main/res/layout/item_suggestion_chip.xml`
- `LauncherAI/app/src/main/res/values/colors.xml`
- `LauncherAI/app/src/main/res/values/themes.xml`
- `LauncherAI/app/src/main/res/drawable/bg_ai_bubble.xml`
- `LauncherAI/app/src/main/res/drawable/bg_tool_card.xml`
- `LauncherAI/app/src/main/res/drawable/bg_user_bubble.xml`
- `LauncherAI/app/src/main/res/drawable/bg_card_rounded.xml`

### Evidence ledger and limitations

| Finding | Reference evidence | LauncherAI evidence | Type/confidence |
|---|---|---|---|
| Active chat rendering path | `AiChatActivity` constructs `AiChatView`; V1 wiring shows RecyclerView/adapters/input. | `NovaAiChatActivity` inflates `activity_ai_chat.xml` and binds `ChatAdapter`. | Direct source/resource; confirmed |
| Reference request/continuation | `AiClient` `api/chat/run`, SSE parser, auth retry, `runWithHitl`, continuation methods. | `NovaAiEngine.submitPrompt` calls `streamPrompt(prompt)` and executes local tools without follow-up. | Direct source; confirmed |
| Dynamic model selector | `ConversationApiClient` and `ModelSelectorSheet` fetch and render `SelectableModelsResponse`. | `NovaAiSettingsActivity` hardcodes model choices and stores integer index. | Direct source; confirmed |
| Native tool inventory | `AiTool` metadata and tool-family files; additional dynamic families remain unenumerated. | `AiToolBridge` registers five implementation classes. | Direct source; confirmed for listed names, partial for full dynamic inventory |
| HITL argument loss | Reference callback/run path returns tool results to continuation. | Entity lacks args; activity mapping loses them and confirmation uses empty-map fallback. | Direct source; confirmed |
| UI dimensions/colors | Reference colors/dimens resource values; original layout XML/screenshots absent. | Chat/settings/message XML and drawable/color resources. | Resource/XML; confirmed for LauncherAI values, unknown for exact reference geometry |
| DEX-level call graph/runtime | No reference APK/DEX; REA Android provider unavailable due Windows Application Control. | Not required for source path inspection; no runtime run performed. | No DEX Evidence; unknown |
| Screenshot comparison | No reference or LauncherAI screenshots supplied/captured. | Static XML only. | No runtime observation; unknown |

REA tools/analysis methods used: installed REA CLI version check and `rea doctor --provider jadx --json` preflight. Android DEX analysis did not run; no REA Evidence IDs exist. Direct methods used were source/resource inspection and targeted searches of the supplied decompiled tree. No project build, tests, emulator run, or screenshot comparison was performed.

### Closing findings

- **Confirmed matches:** only partial component analogues; no end-to-end MATCHED feature.
- **Missing features:** complete tool-result continuation, durable tool arguments, server-like model selection/conversation flow, true on-device inference, and several active reference UI interactions.
- **Critical discrepancies:** reference server-mediated AI vs LauncherAI direct vendor keys; absent context/continuation; confirmation arguments lost; incorrect blank-key fallback; placeholder on-device inference.
- **Recommended next implementation phase:** an explicitly approved remediation phase beginning with safe tool-call/HITL round-trip and provider-selection correctness, followed by conversation context and UI parity work. Phase 8D remains untouched.
