package com.example.launcherai.gestures

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.view.MotionEvent
import android.view.View
import android.widget.Toast

enum class GestureAction(val label: String) {
    NONE("No Action"),
    APP_DRAWER("Open App Drawer"),
    SETTINGS("Open Launcher Settings"),
    EXPAND_NOTIFICATIONS("Expand Notifications"),
    EXPAND_QUICK_SETTINGS("Expand Quick Settings"),
    SEARCH("Open Search"),
    LOCK_SCREEN("Lock Screen");

    companion object {
        fun fromName(name: String, default: GestureAction = NONE): GestureAction {
            return entries.find { it.name == name } ?: default
        }
    }
}

/**
 * Centralized gesture coordinator wrapping NovaGestureRecognizer and mapping GestureSlot triggers
 * to configured NamedAction targets. Keeps backwards-compatible GestureAction API intact.
 */
class GestureHandler(
    private val context: Context,
    private val onExecuteAction: (GestureAction) -> Unit = {},
    private val onExecuteNamedAction: ((NamedAction) -> Unit)? = null,
    private val hostViewProvider: () -> View? = { (context as? Activity)?.window?.decorView }
) {
    constructor(
        context: Context,
        onExecuteAction: (GestureAction) -> Unit
    ) : this(context, onExecuteAction, null)

    var swipeUpAction: GestureAction = GestureAction.APP_DRAWER
    var swipeDownAction: GestureAction = GestureAction.EXPAND_NOTIFICATIONS
    var doubleTapAction: GestureAction = GestureAction.NONE
    var pinchInAction: GestureAction = GestureAction.NONE
    var twoFingerSwipeUpAction: GestureAction = GestureAction.NONE
    var twoFingerSwipeDownAction: GestureAction = GestureAction.NONE

    private val slotActions = mutableMapOf<GestureSlot, NamedAction>()
    private var legacySwipeDown: Boolean = false
    private var swipeDownHaptic: Boolean = true

    private val recognizer = NovaGestureRecognizer(
        context = context,
        hostViewProvider = hostViewProvider,
        onSlotTriggered = { slot -> dispatchSlot(slot) },
        isSlotBoundTo = { slot, target ->
            val act = getResolvedAction(slot)
            act.kind == NamedAction.Kind.NOVA_ACTION && act.novaAction == target
        },
        isLegacySwipeDown = { legacySwipeDown },
        isSwipeDownHapticEnabled = { swipeDownHaptic }
    )

    fun reloadFromPreferences(prefs: SharedPreferences) {
        val swipeToOpen = prefs.getBoolean("key_drawer_swipe_to_open", true)
        for (slot in GestureSlot.entries) {
            slotActions[slot] = GesturePreferences.getAction(prefs, slot)
        }
        if (!swipeToOpen && slotActions[GestureSlot.SWIPE_UP]?.novaAction == NovaAction.APP_DRAWER) {
            // Preserve user's explicit swipe-to-open toggle override for App Drawer
            slotActions[GestureSlot.SWIPE_UP] = NamedAction.NONE
        }
        legacySwipeDown = GesturePreferences.isSwipeDownLegacy(prefs)
        swipeDownHaptic = GesturePreferences.isSwipeDownHaptic(prefs)
    }

    fun getResolvedAction(slot: GestureSlot): NamedAction {
        slotActions[slot]?.let { return it }
        return when (slot) {
            GestureSlot.SWIPE_UP -> NamedAction.fromLegacyGestureAction(swipeUpAction)
            GestureSlot.SWIPE_DOWN -> NamedAction.fromLegacyGestureAction(swipeDownAction)
            GestureSlot.DOUBLE_TAP -> NamedAction.fromLegacyGestureAction(doubleTapAction)
            GestureSlot.PINCH_IN -> NamedAction.fromLegacyGestureAction(pinchInAction)
            GestureSlot.TWO_FINGER_SWIPE_UP -> NamedAction.fromLegacyGestureAction(twoFingerSwipeUpAction)
            GestureSlot.TWO_FINGER_SWIPE_DOWN -> NamedAction.fromLegacyGestureAction(twoFingerSwipeDownAction)
            else -> NamedAction.NONE
        }
    }

    fun setItemSuppressionMask(mask: Int) {
        recognizer.setItemSuppressionMask(mask)
    }

    fun shouldDragDrawerOnSwipeUp(): Boolean {
        val act = getResolvedAction(GestureSlot.SWIPE_UP)
        val prefs = context.getSharedPreferences(GesturePreferences.PREFS_NAME, Context.MODE_PRIVATE)
        val swipeToOpen = prefs.getBoolean("key_drawer_swipe_to_open", true)
        return swipeToOpen && act.kind == NamedAction.Kind.NOVA_ACTION && act.novaAction == NovaAction.APP_DRAWER
    }

    private fun dispatchSlot(slot: GestureSlot) {
        val named = getResolvedAction(slot)
        if (named.isNone) return
        if (onExecuteNamedAction != null) {
            onExecuteNamedAction.invoke(named)
        } else if (named.kind == NamedAction.Kind.NOVA_ACTION) {
            val legacy = toLegacyAction(named.novaAction)
            if (legacy != null) onExecuteAction(legacy)
        }
    }

    fun triggerSwipeUp() {
        dispatchSlot(GestureSlot.SWIPE_UP)
    }

    fun triggerSlot(slot: GestureSlot): Boolean {
        val act = getResolvedAction(slot)
        if (act.isNone) return false
        dispatchSlot(slot)
        return true
    }

    fun onTouchEvent(event: MotionEvent): Boolean = recognizer.onTouchEvent(event)

    private fun toLegacyAction(action: NovaAction): GestureAction? {
        return when (action) {
            NovaAction.NONE -> GestureAction.NONE
            NovaAction.APP_DRAWER -> GestureAction.APP_DRAWER
            NovaAction.NOVA_SETTINGS -> GestureAction.SETTINGS
            NovaAction.EXPAND_STATUS_BAR -> GestureAction.EXPAND_NOTIFICATIONS
            NovaAction.EXPAND_STATUS_SETTINGS_BAR -> GestureAction.EXPAND_QUICK_SETTINGS
            NovaAction.APP_SEARCH -> GestureAction.SEARCH
            NovaAction.SCREEN_OFF -> GestureAction.LOCK_SCREEN
            else -> null
        }
    }

    companion object {
        fun executeSystemAction(context: Context, action: GestureAction) {
            when (action) {
                GestureAction.EXPAND_NOTIFICATIONS -> expandNotifications(context)
                GestureAction.EXPAND_QUICK_SETTINGS -> expandQuickSettings(context)
                GestureAction.LOCK_SCREEN -> ScreenLockHelper.lockScreen(context)
                else -> {}
            }
        }

        fun expandNotifications(context: Context) {
            if (tryStatusBarMethod(context, "expandNotificationsPanel")) return
            if (NovaAccessibilityService.expandNotifications()) return
            Toast.makeText(
                context,
                "Enable Accessibility service in Settings if notifications panel is restricted",
                Toast.LENGTH_SHORT
            ).show()
        }

        fun expandQuickSettings(context: Context) {
            if (tryStatusBarMethod(context, "expandSettingsPanel")) return
            if (NovaAccessibilityService.expandQuickSettings()) return
            Toast.makeText(
                context,
                "Enable Accessibility service in Settings if quick settings is restricted",
                Toast.LENGTH_SHORT
            ).show()
        }

        private fun tryStatusBarMethod(context: Context, methodName: String): Boolean {
            return try {
                @Suppress("WrongConstant")
                val sbService = context.getSystemService("statusbar") ?: return false
                val sbClass = Class.forName("android.app.StatusBarManager")
                sbClass.getMethod(methodName).invoke(sbService)
                true
            } catch (_: Exception) {
                false
            }
        }
    }
}
