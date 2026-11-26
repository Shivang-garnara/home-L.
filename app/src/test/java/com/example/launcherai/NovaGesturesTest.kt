package com.example.launcherai

import android.content.SharedPreferences
import com.example.launcherai.data.db.WorkspaceItemEntity
import com.example.launcherai.gestures.GestureAction
import com.example.launcherai.gestures.GestureMath
import com.example.launcherai.gestures.GesturePreferences
import com.example.launcherai.gestures.GestureSlot
import com.example.launcherai.gestures.NamedAction
import com.example.launcherai.gestures.NovaAction
import com.example.launcherai.gestures.ScreenLockMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

class NovaGesturesTest {

    private class FakePrefs(initial: Map<String, Any?> = emptyMap()) : SharedPreferences {
        private val store = initial.toMutableMap()

        override fun getAll(): MutableMap<String, *> = store.toMutableMap()
        override fun getString(key: String?, defValue: String?): String? =
            (store[key] as? String) ?: defValue

        @Suppress("UNCHECKED_CAST")
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            (store[key] as? Set<String>)?.toMutableSet() ?: defValues

        override fun getInt(key: String?, defValue: Int): Int =
            (store[key] as? Int) ?: defValue

        override fun getLong(key: String?, defValue: Long): Long =
            (store[key] as? Long) ?: defValue

        override fun getFloat(key: String?, defValue: Float): Float =
            (store[key] as? Float) ?: defValue

        override fun getBoolean(key: String?, defValue: Boolean): Boolean =
            (store[key] as? Boolean) ?: defValue

        override fun contains(key: String?): Boolean = store.containsKey(key)

        override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
            private val pending = mutableMapOf<String, Any?>()
            private var clearAll = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor {
                if (key != null) pending[key] = value
                return this
            }

            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
                if (key != null) pending[key] = values?.toSet()
                return this
            }

            override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
                if (key != null) pending[key] = value
                return this
            }

            override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
                if (key != null) pending[key] = value
                return this
            }

            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
                if (key != null) pending[key] = value
                return this
            }

            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
                if (key != null) pending[key] = value
                return this
            }

            override fun remove(key: String?): SharedPreferences.Editor {
                if (key != null) pending[key] = null
                return this
            }

            override fun clear(): SharedPreferences.Editor {
                clearAll = true
                return this
            }

            override fun commit(): Boolean {
                apply()
                return true
            }

            override fun apply() {
                if (clearAll) store.clear()
                for ((k, v) in pending) {
                    if (v == null) store.remove(k) else store[k] = v
                }
            }
        }

        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    }

    @Test
    fun namedActionRoundtripSerializationAndSanitization() {
        val none = NamedAction.NONE
        assertTrue(NamedAction.deserialize(none.serialize()).isNone)

        val drawer = NamedAction.nova(NovaAction.APP_DRAWER)
        val drawerRt = NamedAction.deserialize(drawer.serialize())
        assertEquals(NamedAction.Kind.NOVA_ACTION, drawerRt.kind)
        assertEquals(NovaAction.APP_DRAWER, drawerRt.novaAction)
        assertEquals("App drawer", drawerRt.displaySummary())

        val gotoPage = NamedAction.nova(NovaAction.GOTO_SCREEN, pageIndex = 2)
        val gotoRt = NamedAction.deserialize(gotoPage.serialize())
        assertEquals(NovaAction.GOTO_SCREEN, gotoRt.novaAction)
        assertEquals(2, gotoRt.pageIndex)
        assertEquals("Jump to page (Page 3)", gotoRt.displaySummary())

        val appWithDollar = NamedAction.app("Cash\$App", "com.example.cash", "com.example.cash.Main")
        val appSerialized = appWithDollar.serialize()
        assertTrue(appSerialized.startsWith("Cash_App\$app:"))
        val appRt = NamedAction.deserialize(appSerialized)
        assertEquals(NamedAction.Kind.APP, appRt.kind)
        assertEquals("Cash_App", appRt.label)
        assertEquals("com.example.cash", appRt.packageName)
        assertEquals("com.example.cash.Main", appRt.className)

        val uriAction = NamedAction(label = "Web", kind = NamedAction.Kind.INTENT_URI, intentUri = "https://example.com")
        val uriRt = NamedAction.deserialize(uriAction.serialize())
        assertEquals(NamedAction.Kind.INTENT_URI, uriRt.kind)
        assertEquals("https://example.com", uriRt.intentUri)
    }

    @Test
    fun gesturePreferencesLegacyMigrationAndSync() {
        val prefs = FakePrefs(
            mapOf(
                "key_gesture_swipe_up" to GestureAction.SETTINGS.name,
                "key_gesture_swipe_down" to GestureAction.EXPAND_QUICK_SETTINGS.name,
                "key_gesture_double_tap" to GestureAction.LOCK_SCREEN.name,
                "key_gesture_pinch_in" to GestureAction.SEARCH.name
            )
        )

        assertEquals(NovaAction.NOVA_SETTINGS, GesturePreferences.getAction(prefs, GestureSlot.SWIPE_UP).novaAction)
        assertEquals(NovaAction.EXPAND_STATUS_SETTINGS_BAR, GesturePreferences.getAction(prefs, GestureSlot.SWIPE_DOWN).novaAction)
        assertEquals(NovaAction.SCREEN_OFF, GesturePreferences.getAction(prefs, GestureSlot.DOUBLE_TAP).novaAction)
        assertEquals(NovaAction.APP_SEARCH, GesturePreferences.getAction(prefs, GestureSlot.PINCH_IN).novaAction)
        assertTrue(GesturePreferences.getAction(prefs, GestureSlot.SWIPE_LEFT).isNone)

        GesturePreferences.setAction(prefs, GestureSlot.DOUBLE_TAP, NamedAction.nova(NovaAction.RECENT_APPS))
        assertEquals(NovaAction.RECENT_APPS, GesturePreferences.getAction(prefs, GestureSlot.DOUBLE_TAP).novaAction)
        assertEquals(GestureSlot.DOUBLE_TAP, GesturePreferences.findConfiguredSlot(prefs, NovaAction.RECENT_APPS))
        assertNull(GesturePreferences.findConfiguredSlot(prefs, NovaAction.TOGGLE_DOCK))
    }

    @Test
    fun gesturePreferencesDefaultsTogglesAndMenuGating() {
        val prefs = FakePrefs()
        assertEquals(NovaAction.APP_DRAWER, GesturePreferences.getAction(prefs, GestureSlot.SWIPE_UP).novaAction)
        assertEquals(NovaAction.EXPAND_STATUS_BAR, GesturePreferences.getAction(prefs, GestureSlot.SWIPE_DOWN).novaAction)
        assertTrue(GesturePreferences.isHomeButtonOnlyOnDefault(prefs))
        assertTrue(GesturePreferences.isSwipeDownHaptic(prefs))
        assertFalse(GesturePreferences.isSwipeDownLegacy(prefs))
        assertEquals(ScreenLockMethod.AUTO, GesturePreferences.getScreenLockMethod(prefs))

        assertTrue(
            GesturePreferences.shouldShowLongPressMenu(
                hasPermanentMenuKey = true,
                hasSystemLongPressProp = false,
                forceShow = false
            )
        )
        assertFalse(
            GesturePreferences.shouldShowLongPressMenu(
                hasPermanentMenuKey = false,
                hasSystemLongPressProp = false,
                forceShow = false
            )
        )
        assertFalse(
            GesturePreferences.shouldShowLongPressMenu(
                hasPermanentMenuKey = true,
                hasSystemLongPressProp = true,
                forceShow = false
            )
        )
        assertTrue(
            GesturePreferences.shouldShowLongPressMenu(
                hasPermanentMenuKey = false,
                hasSystemLongPressProp = true,
                forceShow = true
            )
        )
    }

    @Test
    fun gestureMathAnchoredDoubleTapSwipeThresholds() {
        val span = 400f
        val downY = 1000f
        assertEquals(
            GestureMath.AnchoredSwipeResult.NONE,
            GestureMath.evaluateAnchoredDoubleTapSwipe(span, downY, 920f)
        )
        // scale = 1 + (-125/400) = 0.6875 <= 0.7 -> UP
        assertEquals(
            GestureMath.AnchoredSwipeResult.UP,
            GestureMath.evaluateAnchoredDoubleTapSwipe(span, downY, 875f)
        )
        // scale = 1 + (+125/400) = 1.3125 >= 1.3 -> DOWN
        assertEquals(
            GestureMath.AnchoredSwipeResult.DOWN,
            GestureMath.evaluateAnchoredDoubleTapSwipe(span, downY, 1125f)
        )
    }

    @Test
    fun gestureMathTwoFingerRotatePinchAndSwipe() {
        // Horizontal initial vector (200, 0), rotated CW by ~20 degrees (+Y is down in screen coords)
        val cwAngle = (20.0 * PI / 180.0).toFloat()
        val cwDx = (200.0 * kotlin.math.cos(cwAngle.toDouble())).toFloat()
        val cwDy = (200.0 * kotlin.math.sin(cwAngle.toDouble())).toFloat()
        assertEquals(
            GestureMath.MultiTouchResult.ROTATE_CW,
            GestureMath.evaluateTwoFingerGesture(200f, 0f, cwDx, cwDy, centroidDeltaY = 0f, translateThresholdPx = 100f)
        )

        val ccwDx = cwDx
        val ccwDy = -cwDy
        assertEquals(
            GestureMath.MultiTouchResult.ROTATE_CCW,
            GestureMath.evaluateTwoFingerGesture(200f, 0f, ccwDx, ccwDy, centroidDeltaY = 0f, translateThresholdPx = 100f)
        )

        // Pinch in (200 -> 140 => 0.70 < 0.80)
        assertEquals(
            GestureMath.MultiTouchResult.PINCH_IN,
            GestureMath.evaluateTwoFingerGesture(200f, 0f, 140f, 0f, centroidDeltaY = 0f, translateThresholdPx = 100f)
        )

        // Pinch out (200 -> 260 => 1.30 > 1.20)
        assertEquals(
            GestureMath.MultiTouchResult.PINCH_OUT,
            GestureMath.evaluateTwoFingerGesture(200f, 0f, 260f, 0f, centroidDeltaY = 0f, translateThresholdPx = 100f)
        )

        // Two-finger swipe up / down without pinching or rotating
        assertEquals(
            GestureMath.MultiTouchResult.TWO_FINGER_SWIPE_UP,
            GestureMath.evaluateTwoFingerGesture(200f, 0f, 200f, 0f, centroidDeltaY = -120f, translateThresholdPx = 100f)
        )
        assertEquals(
            GestureMath.MultiTouchResult.TWO_FINGER_SWIPE_DOWN,
            GestureMath.evaluateTwoFingerGesture(200f, 0f, 200f, 0f, centroidDeltaY = 120f, translateThresholdPx = 100f)
        )
    }

    @Test
    fun gestureMathIncrementalPullAndEdgePullThresholds() {
        assertFalse(GestureMath.shouldTriggerIncrementalSwipeDown(20f, isRelease = false, immediateThresholdPx = 24f, releaseThresholdPx = 12f))
        assertTrue(GestureMath.shouldTriggerIncrementalSwipeDown(25f, isRelease = false, immediateThresholdPx = 24f, releaseThresholdPx = 12f))
        assertFalse(GestureMath.shouldTriggerIncrementalSwipeDown(10f, isRelease = true, immediateThresholdPx = 24f, releaseThresholdPx = 12f))
        assertTrue(GestureMath.shouldTriggerIncrementalSwipeDown(15f, isRelease = true, immediateThresholdPx = 24f, releaseThresholdPx = 12f))

        assertFalse(GestureMath.shouldTriggerEdgePull(0.25f))
        assertTrue(GestureMath.shouldTriggerEdgePull(0.30f))
        assertTrue(GestureMath.shouldTriggerEdgePull(0.45f))
    }

    @Test
    fun workspaceItemEntityCarriesSwipeUpAndDownActions() {
        val base = WorkspaceItemEntity(
            id = 42L,
            itemType = WorkspaceItemEntity.ITEM_TYPE_FOLDER,
            title = "Tools",
            swipeUpAction = NamedAction.nova(NovaAction.FIRST_ITEM_IN_FOLDER).serialize(),
            swipeDownAction = NamedAction.app("Calc", "com.android.calculator2", "com.android.calculator2.Calculator").serialize()
        )
        assertEquals("First item in folder", NamedAction.deserialize(base.swipeUpAction).displaySummary())
        assertEquals("Calc", NamedAction.deserialize(base.swipeDownAction).displaySummary())
    }
}
