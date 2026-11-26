package com.example.launcherai.gestures

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.provider.Settings

/**
 * Built-in Nova Launcher actions matching Nova 8.9.9 (kx.g0).
 */
enum class NovaAction(val title: String) {
    NONE("None"),
    APP_DRAWER("App drawer"),
    DEFAULT_SCREEN("Default page"),
    GOTO_SCREEN("Jump to page"),
    EXPAND_STATUS_BAR("Expand notifications"),
    EXPAND_STATUS_SETTINGS_BAR("Expand quick settings"),
    TOGGLE_STATUS_BAR("Toggle notification bar"),
    TOGGLE_DOCK("Toggle dock"),
    SHOW_PREVIEWS("Desktop previews"),
    RECENT_APPS("Recent apps"),
    APP_SEARCH("Nova Search"),
    TEXT_SEARCH("Search"),
    VOICE_SEARCH("Voice search"),
    ASSIST("Assistant"),
    GOOGLE_LENS("Google Lens"),
    GOOGLE_NOW("Google Discover"),
    NOVA_SETTINGS("Nova Settings"),
    SCREEN_OFF("Screen lock"),
    FIRST_ITEM_IN_FOLDER("First item in folder"),
    OPEN_FOLDER("Open folder");

    companion object {
        val PICKER_ACTIONS: List<NovaAction> = listOf(
            NONE,
            APP_DRAWER,
            DEFAULT_SCREEN,
            GOTO_SCREEN,
            EXPAND_STATUS_BAR,
            EXPAND_STATUS_SETTINGS_BAR,
            TOGGLE_STATUS_BAR,
            TOGGLE_DOCK,
            SHOW_PREVIEWS,
            RECENT_APPS,
            APP_SEARCH,
            TEXT_SEARCH,
            VOICE_SEARCH,
            ASSIST,
            GOOGLE_LENS,
            GOOGLE_NOW,
            NOVA_SETTINGS,
            SCREEN_OFF
        )

        val FOLDER_PICKER_ACTIONS: List<NovaAction> = listOf(
            NONE,
            OPEN_FOLDER,
            FIRST_ITEM_IN_FOLDER,
            APP_DRAWER,
            APP_SEARCH,
            EXPAND_STATUS_BAR,
            SCREEN_OFF,
            NOVA_SETTINGS
        )

        fun fromName(name: String?): NovaAction? {
            if (name.isNullOrBlank()) return null
            return entries.firstOrNull { it.name == name }
        }
    }
}

/**
 * All configurable gesture and hardware input slots in Nova Gestures & inputs.
 */
enum class GestureSlot(
    val prefKey: String,
    val title: String,
    val isButtonInput: Boolean = false
) {
    HOME_BUTTON("key_gesture_home_button_action", "Home button", isButtonInput = true),
    LONG_PRESS_MENU("key_gesture_long_menu_action", "Long-press Menu", isButtonInput = true),
    SWIPE_UP("key_gesture_v2_swipe_up", "Swipe up"),
    SWIPE_DOWN("key_gesture_v2_swipe_down", "Swipe down"),
    SWIPE_LEFT("key_gesture_v2_swipe_left", "Swipe left"),
    SWIPE_RIGHT("key_gesture_v2_swipe_right", "Swipe right"),
    DOUBLE_TAP("key_gesture_v2_double_tap", "Double tap"),
    DOUBLE_TAP_SWIPE_UP("key_gesture_v2_double_tap_swipe_up", "Double tap + swipe up"),
    DOUBLE_TAP_SWIPE_DOWN("key_gesture_v2_double_tap_swipe_down", "Double tap + swipe down"),
    PINCH_IN("key_gesture_v2_pinch_in", "Pinch in"),
    PINCH_OUT("key_gesture_v2_pinch_out", "Pinch out"),
    TWO_FINGER_SWIPE_UP("key_gesture_v2_two_finger_swipe_up", "Two finger swipe up"),
    TWO_FINGER_SWIPE_DOWN("key_gesture_v2_two_finger_swipe_down", "Two finger swipe down"),
    ROTATE_CW("key_gesture_v2_rotate_cw", "Two finger rotation (clockwise)"),
    ROTATE_CCW("key_gesture_v2_rotate_ccw", "Two finger rotation (counterclockwise)")
}

enum class ScreenLockMethod(val title: String, val summary: String) {
    AUTO("Automatic", "Uses Accessibility on Android 9+ or Device Admin if enabled"),
    ACCESSIBILITY(
        "Accessibility",
        "Uses Accessibility service to lock without breaking fingerprint/biometric unlock (Android 9+)"
    ),
    DEVICE_ADMIN(
        "Device Admin",
        "Uses Device Admin force-lock API (may require PIN after lock on some devices)"
    ),
    TIMEOUT(
        "Timeout",
        "Turns screen timeout to minimum and dims screen until display sleeps"
    );

    companion object {
        fun fromName(name: String?): ScreenLockMethod {
            return entries.firstOrNull { it.name == name } ?: AUTO
        }
    }
}

/**
 * Serializable target bound to any gesture, hardware button, or icon/folder swipe action.
 * Format mirrors Nova NamedIntent (`<label>$<uri/scheme>`), sanitizing `$` in labels to `_`.
 */
data class NamedAction(
    val label: String,
    val kind: Kind,
    val novaAction: NovaAction = NovaAction.NONE,
    val packageName: String? = null,
    val className: String? = null,
    val intentUri: String? = null,
    val pageIndex: Int = 0
) {
    enum class Kind {
        NONE,
        NOVA_ACTION,
        APP,
        INTENT_URI
    }

    val isNone: Boolean
        get() = kind == Kind.NONE || (kind == Kind.NOVA_ACTION && novaAction == NovaAction.NONE)

    fun displaySummary(): String {
        if (isNone) return "None"
        return when (kind) {
            Kind.NOVA_ACTION -> {
                if (novaAction == NovaAction.GOTO_SCREEN) {
                    "Jump to page (Page ${pageIndex + 1})"
                } else {
                    label.ifBlank { novaAction.title }
                }
            }
            Kind.APP, Kind.INTENT_URI -> label.ifBlank { "Custom shortcut" }
            Kind.NONE -> "None"
        }
    }

    fun serialize(): String {
        if (isNone) return "None\$none:"
        val safeLabel = label.replace('$', '_')
        return when (kind) {
            Kind.NOVA_ACTION -> {
                if (novaAction == NovaAction.GOTO_SCREEN) {
                    "$safeLabel\$nova:${novaAction.name}:$pageIndex"
                } else {
                    "$safeLabel\$nova:${novaAction.name}"
                }
            }
            Kind.APP -> "$safeLabel\$app:${packageName.orEmpty()}/${className.orEmpty()}"
            Kind.INTENT_URI -> "$safeLabel\$uri:${intentUri.orEmpty()}"
            Kind.NONE -> "None\$none:"
        }
    }

    fun toLaunchIntent(context: Context): Intent? {
        return when (kind) {
            Kind.NONE -> null
            Kind.APP -> {
                val pkg = packageName ?: return null
                val cls = className
                if (!cls.isNullOrBlank()) {
                    Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_LAUNCHER)
                        setClassName(pkg, cls)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    }
                } else {
                    context.packageManager.getLaunchIntentForPackage(pkg)?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
            }
            Kind.INTENT_URI -> {
                val raw = intentUri ?: return null
                try {
                    Intent.parseUri(raw, Intent.URI_INTENT_SCHEME).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                } catch (_: Exception) {
                    null
                }
            }
            Kind.NOVA_ACTION -> null
        }
    }

    companion object {
        val NONE = NamedAction(label = "None", kind = Kind.NONE, novaAction = NovaAction.NONE)

        fun nova(action: NovaAction, pageIndex: Int = 0): NamedAction {
            if (action == NovaAction.NONE) return NONE
            val lbl = if (action == NovaAction.GOTO_SCREEN) {
                "Page ${pageIndex + 1}"
            } else {
                action.title
            }
            return NamedAction(
                label = lbl,
                kind = Kind.NOVA_ACTION,
                novaAction = action,
                pageIndex = pageIndex
            )
        }

        fun app(label: String, packageName: String, className: String): NamedAction {
            return NamedAction(
                label = label.replace('$', '_'),
                kind = Kind.APP,
                packageName = packageName,
                className = className
            )
        }

        fun fromIntent(label: String, intent: Intent): NamedAction {
            val safeName = label.ifBlank { "Shortcut" }.replace('$', '_')
            val sanitized = Intent(intent)
            // Nova fh.f.z promotes ACTION_DIAL direct tel intents when launched as shortcuts
            if (sanitized.action == Intent.ACTION_CALL && sanitized.data != null) {
                sanitized.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            return NamedAction(
                label = safeName,
                kind = Kind.INTENT_URI,
                intentUri = sanitized.toUri(Intent.URI_INTENT_SCHEME)
            )
        }

        fun deserialize(raw: String?): NamedAction {
            if (raw.isNullOrBlank()) return NONE
            val idx = raw.indexOf('$')
            if (idx < 0 || idx >= raw.length - 1) {
                val legacy = GestureAction.entries.firstOrNull { it.name == raw }
                if (legacy != null) return fromLegacyGestureAction(legacy)
                val directNova = NovaAction.fromName(raw)
                return if (directNova != null) nova(directNova) else NONE
            }
            val label = raw.substring(0, idx)
            val payload = raw.substring(idx + 1)
            return when {
                payload.startsWith("none:") -> NONE
                payload.startsWith("nova:") -> {
                    val rest = payload.removePrefix("nova:")
                    val colon = rest.indexOf(':')
                    val actionName = if (colon >= 0) rest.substring(0, colon) else rest
                    val pageIdx = if (colon >= 0) rest.substring(colon + 1).toIntOrNull() ?: 0 else 0
                    val action = NovaAction.fromName(actionName) ?: return NONE
                    nova(action, pageIdx).copy(label = label.ifBlank { action.title })
                }
                payload.startsWith("app:") -> {
                    val rest = payload.removePrefix("app:")
                    val slash = rest.indexOf('/')
                    val pkg = if (slash >= 0) rest.substring(0, slash) else rest
                    val cls = if (slash >= 0) rest.substring(slash + 1) else ""
                    if (pkg.isBlank()) NONE else app(label, pkg, cls)
                }
                payload.startsWith("uri:") -> {
                    val uri = payload.removePrefix("uri:")
                    if (uri.isBlank()) NONE else NamedAction(label = label, kind = Kind.INTENT_URI, intentUri = uri)
                }
                else -> NONE
            }
        }

        fun fromLegacyGestureAction(action: GestureAction): NamedAction {
            return when (action) {
                GestureAction.NONE -> NONE
                GestureAction.APP_DRAWER -> nova(NovaAction.APP_DRAWER)
                GestureAction.SETTINGS -> nova(NovaAction.NOVA_SETTINGS)
                GestureAction.EXPAND_NOTIFICATIONS -> nova(NovaAction.EXPAND_STATUS_BAR)
                GestureAction.EXPAND_QUICK_SETTINGS -> nova(NovaAction.EXPAND_STATUS_SETTINGS_BAR)
                GestureAction.SEARCH -> nova(NovaAction.APP_SEARCH)
                GestureAction.LOCK_SCREEN -> nova(NovaAction.SCREEN_OFF)
            }
        }
    }
}

object GesturePreferences {
    const val PREFS_NAME = "workspace_prefs"
    const val KEY_HOME_BUTTON_ONLY_ON_DEFAULT = "key_gesture_home_button_only_on_default"
    const val KEY_SWIPE_DOWN_HAPTIC = "key_gesture_swipe_down_haptic"
    const val KEY_SWIPE_DOWN_LEGACY = "key_gesture_swipe_down_legacy"
    const val KEY_SCREEN_LOCK_METHOD = "key_screen_lock_method"
    const val KEY_SHOW_LONG_PRESS_MENU_FORCE = "key_gesture_show_long_menu_force"

    fun defaultActionFor(slot: GestureSlot): NamedAction {
        return when (slot) {
            GestureSlot.SWIPE_UP -> NamedAction.nova(NovaAction.APP_DRAWER)
            GestureSlot.SWIPE_DOWN -> NamedAction.nova(NovaAction.EXPAND_STATUS_BAR)
            else -> NamedAction.NONE
        }
    }

    fun getAction(prefs: SharedPreferences, slot: GestureSlot): NamedAction {
        val raw = prefs.getString(slot.prefKey, null)
        if (!raw.isNullOrBlank()) {
            return NamedAction.deserialize(raw)
        }
        // Migrate legacy v1 gesture keys seamlessly
        return when (slot) {
            GestureSlot.SWIPE_UP -> {
                val legacy = prefs.getString("key_gesture_swipe_up", null)
                if (!legacy.isNullOrBlank()) {
                    NamedAction.fromLegacyGestureAction(GestureAction.fromName(legacy, GestureAction.APP_DRAWER))
                } else {
                    defaultActionFor(slot)
                }
            }
            GestureSlot.SWIPE_DOWN -> {
                val legacy = prefs.getString("key_gesture_swipe_down", null)
                if (!legacy.isNullOrBlank()) {
                    NamedAction.fromLegacyGestureAction(GestureAction.fromName(legacy, GestureAction.EXPAND_NOTIFICATIONS))
                } else {
                    defaultActionFor(slot)
                }
            }
            GestureSlot.DOUBLE_TAP -> {
                val legacy = prefs.getString("key_gesture_double_tap", null)
                if (!legacy.isNullOrBlank()) {
                    NamedAction.fromLegacyGestureAction(GestureAction.fromName(legacy, GestureAction.NONE))
                } else {
                    NamedAction.NONE
                }
            }
            GestureSlot.PINCH_IN -> {
                val legacy = prefs.getString("key_gesture_pinch_in", null)
                if (!legacy.isNullOrBlank()) {
                    NamedAction.fromLegacyGestureAction(GestureAction.fromName(legacy, GestureAction.NONE))
                } else {
                    NamedAction.NONE
                }
            }
            GestureSlot.TWO_FINGER_SWIPE_UP -> {
                val legacy = prefs.getString("key_gesture_two_finger_swipe_up", null)
                if (!legacy.isNullOrBlank()) {
                    NamedAction.fromLegacyGestureAction(GestureAction.fromName(legacy, GestureAction.NONE))
                } else {
                    NamedAction.NONE
                }
            }
            GestureSlot.TWO_FINGER_SWIPE_DOWN -> {
                val legacy = prefs.getString("key_gesture_two_finger_swipe_down", null)
                if (!legacy.isNullOrBlank()) {
                    NamedAction.fromLegacyGestureAction(GestureAction.fromName(legacy, GestureAction.NONE))
                } else {
                    NamedAction.NONE
                }
            }
            else -> defaultActionFor(slot)
        }
    }

    fun setAction(prefs: SharedPreferences, slot: GestureSlot, action: NamedAction) {
        val editor = prefs.edit().putString(slot.prefKey, action.serialize())
        // Keep legacy keys in sync when applicable
        val legacyMirror = when {
            action.isNone -> GestureAction.NONE.name
            action.kind == NamedAction.Kind.NOVA_ACTION -> when (action.novaAction) {
                NovaAction.APP_DRAWER -> GestureAction.APP_DRAWER.name
                NovaAction.NOVA_SETTINGS -> GestureAction.SETTINGS.name
                NovaAction.EXPAND_STATUS_BAR -> GestureAction.EXPAND_NOTIFICATIONS.name
                NovaAction.EXPAND_STATUS_SETTINGS_BAR -> GestureAction.EXPAND_QUICK_SETTINGS.name
                NovaAction.APP_SEARCH -> GestureAction.SEARCH.name
                NovaAction.SCREEN_OFF -> GestureAction.LOCK_SCREEN.name
                else -> null
            }
            else -> null
        }
        val legacyKey = when (slot) {
            GestureSlot.SWIPE_UP -> "key_gesture_swipe_up"
            GestureSlot.SWIPE_DOWN -> "key_gesture_swipe_down"
            GestureSlot.DOUBLE_TAP -> "key_gesture_double_tap"
            GestureSlot.PINCH_IN -> "key_gesture_pinch_in"
            GestureSlot.TWO_FINGER_SWIPE_UP -> "key_gesture_two_finger_swipe_up"
            GestureSlot.TWO_FINGER_SWIPE_DOWN -> "key_gesture_two_finger_swipe_down"
            else -> null
        }
        if (legacyKey != null && legacyMirror != null) {
            editor.putString(legacyKey, legacyMirror)
        }
        editor.apply()
    }

    fun isHomeButtonOnlyOnDefault(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(KEY_HOME_BUTTON_ONLY_ON_DEFAULT, true)

    fun setHomeButtonOnlyOnDefault(prefs: SharedPreferences, enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HOME_BUTTON_ONLY_ON_DEFAULT, enabled).apply()
    }

    fun isSwipeDownHaptic(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(KEY_SWIPE_DOWN_HAPTIC, true)

    fun setSwipeDownHaptic(prefs: SharedPreferences, enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SWIPE_DOWN_HAPTIC, enabled).apply()
    }

    fun isSwipeDownLegacy(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(KEY_SWIPE_DOWN_LEGACY, false)

    fun setSwipeDownLegacy(prefs: SharedPreferences, enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SWIPE_DOWN_LEGACY, enabled).apply()
    }

    fun getScreenLockMethod(prefs: SharedPreferences): ScreenLockMethod =
        ScreenLockMethod.fromName(prefs.getString(KEY_SCREEN_LOCK_METHOD, ScreenLockMethod.AUTO.name))

    fun setScreenLockMethod(prefs: SharedPreferences, method: ScreenLockMethod) {
        prefs.edit().putString(KEY_SCREEN_LOCK_METHOD, method.name).apply()
    }

    fun isInfiniteScrollEnabled(prefs: SharedPreferences): Boolean =
        prefs.getBoolean("key_infinite_scroll", false)

    fun hasSystemLongPressMenuSetting(): Boolean {
        return try {
            Settings.System::class.java.getField("KEY_MENU_LONG_PRESS_ACTION")
            true
        } catch (_: NoSuchFieldException) {
            false
        }
    }

    fun shouldShowLongPressMenu(
        hasPermanentMenuKey: Boolean,
        hasSystemLongPressProp: Boolean,
        forceShow: Boolean
    ): Boolean {
        if (forceShow) return true
        return hasPermanentMenuKey && !hasSystemLongPressProp
    }

    fun findConfiguredSlot(prefs: SharedPreferences, target: NovaAction): GestureSlot? {
        return GestureSlot.entries.firstOrNull { slot ->
            val act = getAction(prefs, slot)
            act.kind == NamedAction.Kind.NOVA_ACTION && act.novaAction == target
        }
    }
}
