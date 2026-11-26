package com.example.launcherai.gestures

import android.content.Context
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewConfiguration
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Pure, testable gesture geometry and threshold calculations matching Nova Launcher 8.9.9
 * (fz.h double-tap, fz.j multi-touch + anchored scale, fz.c incremental pull, fz.i edge overscroll).
 */
object GestureMath {

    /** Nova 8.9.9 rotation threshold: 0.261799 rad (~15 degrees). */
    const val ROTATION_THRESHOLD_RAD = 0.261799f

    /** Nova 8.9.9 pinch-in normalized scale threshold (< 0.8f). */
    const val PINCH_IN_THRESHOLD = 0.8f

    /** Nova 8.9.9 pinch-out normalized scale threshold (> 1.2f). */
    const val PINCH_OUT_THRESHOLD = 1.2f

    /** Nova 8.9.9 anchored double-tap + swipe up/down thresholds (<= 0.7f / >= 1.3f). */
    const val ANCHORED_SWIPE_UP_SCALE = 0.7f
    const val ANCHORED_SWIPE_DOWN_SCALE = 1.3f

    /** Nova 8.9.9 double-tap completion max duration (< 180ms down->up). */
    const val DOUBLE_TAP_MAX_HOLD_MS = 180L

    /** Nova 8.9.9 incremental notification pull thresholds (24dp immediate, 12dp on release). */
    const val SWIPE_DOWN_IMMEDIATE_DP = 24f
    const val SWIPE_DOWN_RELEASE_DP = 12f

    /** Nova 8.9.9 two-finger translation threshold (dimen/gesture_translate_threshold = 50dp). */
    const val TWO_FINGER_TRANSLATE_DP = 50f

    /** Nova 8.9.9 Workspace edge pull release threshold (fz.i d >= 0.3f). */
    const val WORKSPACE_EDGE_PULL_THRESHOLD = 0.3f

    enum class MultiTouchResult {
        NONE,
        PINCH_IN,
        PINCH_OUT,
        ROTATE_CW,
        ROTATE_CCW,
        TWO_FINGER_SWIPE_UP,
        TWO_FINGER_SWIPE_DOWN
    }

    enum class AnchoredSwipeResult {
        NONE,
        UP,
        DOWN
    }

    /** Wraps radian difference into [-PI, PI] (mirrors Nova fz.j angle normalization). */
    fun wrapAnglePi(radians: Float): Float {
        var r = radians
        val twoPi = (2.0 * PI).toFloat()
        val pi = PI.toFloat()
        while (r > pi) r -= twoPi
        while (r < -pi) r += twoPi
        return r
    }

    /**
     * Evaluates Nova double-tap + vertical drag in anchored scale mode:
     * Dragging upward shrinks normalized ratio <= 0.7f -> UP; downward grows >= 1.3f -> DOWN.
     */
    fun evaluateAnchoredDoubleTapSwipe(
        anchorSpanPx: Float,
        anchorDownY: Float,
        currentY: Float
    ): AnchoredSwipeResult {
        if (anchorSpanPx <= 0f) return AnchoredSwipeResult.NONE
        val scale = 1f + ((currentY - anchorDownY) / anchorSpanPx)
        return when {
            scale <= ANCHORED_SWIPE_UP_SCALE -> AnchoredSwipeResult.UP
            scale >= ANCHORED_SWIPE_DOWN_SCALE -> AnchoredSwipeResult.DOWN
            else -> AnchoredSwipeResult.NONE
        }
    }

    /**
     * Evaluates simultaneous 2-finger pinch, clockwise/counterclockwise rotation, and 2-finger vertical swipe.
     */
    fun evaluateTwoFingerGesture(
        initialDx: Float,
        initialDy: Float,
        currentDx: Float,
        currentDy: Float,
        centroidDeltaY: Float,
        translateThresholdPx: Float
    ): MultiTouchResult {
        val initialSpan = hypot(initialDx.toDouble(), initialDy.toDouble()).toFloat()
        val currentSpan = hypot(currentDx.toDouble(), currentDy.toDouble()).toFloat()
        if (initialSpan <= 1f) return MultiTouchResult.NONE

        val scale = currentSpan / initialSpan
        val initialAngle = atan2(initialDy.toDouble(), initialDx.toDouble()).toFloat()
        val currentAngle = atan2(currentDy.toDouble(), currentDx.toDouble()).toFloat()
        val deltaAngle = wrapAnglePi(currentAngle - initialAngle)

        // Screen coordinates have +Y downward: clockwise geometric vector change corresponds to +deltaAngle
        if (abs(deltaAngle) > ROTATION_THRESHOLD_RAD && abs(scale - 1f) < 0.25f) {
            return if (deltaAngle < 0f) MultiTouchResult.ROTATE_CCW else MultiTouchResult.ROTATE_CW
        }

        if (scale < PINCH_IN_THRESHOLD) {
            return MultiTouchResult.PINCH_IN
        }
        if (scale > PINCH_OUT_THRESHOLD && scale < 2.8f) {
            return MultiTouchResult.PINCH_OUT
        }

        if (abs(centroidDeltaY) > translateThresholdPx && abs(scale - 1f) < 0.35f) {
            return if (centroidDeltaY < 0f) {
                MultiTouchResult.TWO_FINGER_SWIPE_UP
            } else {
                MultiTouchResult.TWO_FINGER_SWIPE_DOWN
            }
        }
        return MultiTouchResult.NONE
    }

    fun shouldTriggerIncrementalSwipeDown(
        deltaY: Float,
        isRelease: Boolean,
        immediateThresholdPx: Float,
        releaseThresholdPx: Float
    ): Boolean {
        return if (isRelease) deltaY > releaseThresholdPx else deltaY > immediateThresholdPx
    }

    fun shouldTriggerEdgePull(pullFraction: Float): Boolean =
        pullFraction >= WORKSPACE_EDGE_PULL_THRESHOLD
}

/**
 * Full Nova-parity multi-touch, double-tap, double-tap+swipe, two-finger rotate/pinch/swipe,
 * and incremental swipe-down recognizer backed by DragLayer coordinates.
 */
class NovaGestureRecognizer(
    private val context: Context,
    private val hostViewProvider: () -> View?,
    private val onSlotTriggered: (GestureSlot) -> Unit,
    private val isSlotBoundTo: (GestureSlot, NovaAction) -> Boolean,
    private val isLegacySwipeDown: () -> Boolean,
    private val isSwipeDownHapticEnabled: () -> Boolean
) {
    private val density = context.resources.displayMetrics.density
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val doubleTapSlopSq = ViewConfiguration.get(context).scaledDoubleTapSlop.let { it * it }
    private val anchorSpanPx = (160f * density).coerceAtLeast(100f)
    private val twoFingerTranslateThresholdPx = GestureMath.TWO_FINGER_TRANSLATE_DP * density
    private val swipeDownImmediatePx = GestureMath.SWIPE_DOWN_IMMEDIATE_DP * density
    private val swipeDownReleasePx = GestureMath.SWIPE_DOWN_RELEASE_DP * density

    private var lockedForCurrentSequence = false
    private var twoFingerActive = false
    private var twoFingerStartDx = 0f
    private var twoFingerStartDy = 0f
    private var twoFingerStartCentroidY = 0f
    private var twoFingerMovedSinceDown = false

    private var singleDownX = 0f
    private var singleDownY = 0f
    private var maxPointerCountSeen = 0
    private var singleFingerDragged = false
    private var incrementalDownFired = false

    // Double-tap + swipe anchored state
    private var inDoubleTapCandidate = false
    private var doubleTapDownTime = 0L
    private var doubleTapDownX = 0f
    private var doubleTapDownY = 0f
    private var doubleTapEnteredAnchoredSwipe = false
    private var lastDoubleTapConsumedTime = 0L

    private var suppressionMask = 0

    fun setItemSuppressionMask(mask: Int) {
        suppressionMask = mask
    }

    private val simpleGestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (lockedForCurrentSequence || maxPointerCountSeen > 1 || e1 == null) return false
                val dy = e2.y - e1.y
                val dx = e2.x - e1.x
                if (abs(dy) <= abs(dx) * 1.15f) return false
                val sufficient = abs(dy) > touchSlop * 2f || abs(velocityY) > 250f
                if (!sufficient) return false

                if (dy < 0f && (suppressionMask and FLAG_SUPPRESS_SWIPE_UP) == 0) {
                    lockedForCurrentSequence = true
                    onSlotTriggered(GestureSlot.SWIPE_UP)
                    return true
                }
                if (dy > 0f && (suppressionMask and FLAG_SUPPRESS_SWIPE_DOWN) == 0 && !incrementalDownFired) {
                    lockedForCurrentSequence = true
                    onSlotTriggered(GestureSlot.SWIPE_DOWN)
                    return true
                }
                return false
            }
        }
    ).apply {
        setOnDoubleTapListener(object : GestureDetector.OnDoubleTapListener {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean = false
            override fun onDoubleTap(e: MotionEvent): Boolean {
                inDoubleTapCandidate = true
                doubleTapDownTime = e.downTime
                doubleTapDownX = e.x
                doubleTapDownY = e.y
                doubleTapEnteredAnchoredSwipe = false
                return true
            }

            override fun onDoubleTapEvent(e: MotionEvent): Boolean {
                if (!inDoubleTapCandidate || lockedForCurrentSequence || maxPointerCountSeen > 1) return false
                when (e.actionMasked) {
                    MotionEvent.ACTION_MOVE -> {
                        val movedSq = (e.x - doubleTapDownX).let { it * it } + (e.y - doubleTapDownY).let { it * it }
                        if (movedSq > doubleTapSlopSq * 0.25f) {
                            doubleTapEnteredAnchoredSwipe = true
                        }
                        if (doubleTapEnteredAnchoredSwipe) {
                            val dir = GestureMath.evaluateAnchoredDoubleTapSwipe(
                                anchorSpanPx = anchorSpanPx,
                                anchorDownY = doubleTapDownY,
                                currentY = e.y
                            )
                            if (dir == GestureMath.AnchoredSwipeResult.UP) {
                                lockedForCurrentSequence = true
                                inDoubleTapCandidate = false
                                lastDoubleTapConsumedTime = e.eventTime
                                onSlotTriggered(GestureSlot.DOUBLE_TAP_SWIPE_UP)
                                return true
                            } else if (dir == GestureMath.AnchoredSwipeResult.DOWN) {
                                lockedForCurrentSequence = true
                                inDoubleTapCandidate = false
                                lastDoubleTapConsumedTime = e.eventTime
                                onSlotTriggered(GestureSlot.DOUBLE_TAP_SWIPE_DOWN)
                                return true
                            }
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        val elapsed = e.eventTime - doubleTapDownTime
                        if (!doubleTapEnteredAnchoredSwipe &&
                            elapsed < GestureMath.DOUBLE_TAP_MAX_HOLD_MS &&
                            e.eventTime > lastDoubleTapConsumedTime + 100L
                        ) {
                            lockedForCurrentSequence = true
                            inDoubleTapCandidate = false
                            lastDoubleTapConsumedTime = e.eventTime
                            onSlotTriggered(GestureSlot.DOUBLE_TAP)
                            return true
                        }
                        inDoubleTapCandidate = false
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        inDoubleTapCandidate = false
                    }
                }
                return false
            }
        })
    }

    fun onTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lockedForCurrentSequence = false
                twoFingerActive = false
                twoFingerMovedSinceDown = false
                maxPointerCountSeen = 1
                singleDownX = ev.x
                singleDownY = ev.y
                singleFingerDragged = false
                incrementalDownFired = false
                inDoubleTapCandidate = false
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                maxPointerCountSeen = maxOf(maxPointerCountSeen, ev.pointerCount)
                if (ev.pointerCount == 2 && !lockedForCurrentSequence) {
                    twoFingerActive = true
                    twoFingerStartDx = ev.getX(1) - ev.getX(0)
                    twoFingerStartDy = ev.getY(1) - ev.getY(0)
                    twoFingerStartCentroidY = (ev.getY(0) + ev.getY(1)) * 0.5f
                    twoFingerMovedSinceDown = false
                }
            }

            MotionEvent.ACTION_MOVE -> {
                maxPointerCountSeen = maxOf(maxPointerCountSeen, ev.pointerCount)
                if (!lockedForCurrentSequence) {
                    if (ev.pointerCount >= 2) {
                        if (!twoFingerActive) {
                            twoFingerActive = true
                            twoFingerStartDx = ev.getX(1) - ev.getX(0)
                            twoFingerStartDy = ev.getY(1) - ev.getY(0)
                            twoFingerStartCentroidY = (ev.getY(0) + ev.getY(1)) * 0.5f
                        }
                        val curDx = ev.getX(1) - ev.getX(0)
                        val curDy = ev.getY(1) - ev.getY(0)
                        val curCentroidY = (ev.getY(0) + ev.getY(1)) * 0.5f
                        val centroidDeltaY = curCentroidY - twoFingerStartCentroidY

                        val result = GestureMath.evaluateTwoFingerGesture(
                            initialDx = twoFingerStartDx,
                            initialDy = twoFingerStartDy,
                            currentDx = curDx,
                            currentDy = curDy,
                            centroidDeltaY = centroidDeltaY,
                            translateThresholdPx = twoFingerTranslateThresholdPx
                        )
                        val triggeredSlot = when (result) {
                            GestureMath.MultiTouchResult.PINCH_IN -> GestureSlot.PINCH_IN
                            GestureMath.MultiTouchResult.PINCH_OUT -> GestureSlot.PINCH_OUT
                            GestureMath.MultiTouchResult.ROTATE_CW -> GestureSlot.ROTATE_CW
                            GestureMath.MultiTouchResult.ROTATE_CCW -> GestureSlot.ROTATE_CCW
                            GestureMath.MultiTouchResult.TWO_FINGER_SWIPE_UP -> GestureSlot.TWO_FINGER_SWIPE_UP
                            GestureMath.MultiTouchResult.TWO_FINGER_SWIPE_DOWN -> GestureSlot.TWO_FINGER_SWIPE_DOWN
                            GestureMath.MultiTouchResult.NONE -> null
                        }
                        if (triggeredSlot != null) {
                            lockedForCurrentSequence = true
                            twoFingerActive = false
                            onSlotTriggered(triggeredSlot)
                            return true
                        }
                    } else if (!twoFingerActive) {
                        val dx = ev.x - singleDownX
                        val dy = ev.y - singleDownY
                        if (hypot(dx.toDouble(), dy.toDouble()) > touchSlop) {
                            singleFingerDragged = true
                        }
                        // Nova fz.c incremental Swipe Down -> Expand notifications immediately > 24dp
                        if (!incrementalDownFired &&
                            !isLegacySwipeDown() &&
                            (suppressionMask and FLAG_SUPPRESS_SWIPE_DOWN) == 0 &&
                            isSlotBoundTo(GestureSlot.SWIPE_DOWN, NovaAction.EXPAND_STATUS_BAR) &&
                            dy > 0f &&
                            abs(dy) > abs(dx) * 1.25f &&
                            GestureMath.shouldTriggerIncrementalSwipeDown(
                                deltaY = dy,
                                isRelease = false,
                                immediateThresholdPx = swipeDownImmediatePx,
                                releaseThresholdPx = swipeDownReleasePx
                            )
                        ) {
                            incrementalDownFired = true
                            lockedForCurrentSequence = true
                            if (isSwipeDownHapticEnabled()) {
                                hostViewProvider()?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            }
                            onSlotTriggered(GestureSlot.SWIPE_DOWN)
                            return true
                        }
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                if (!lockedForCurrentSequence &&
                    maxPointerCountSeen == 1 &&
                    !incrementalDownFired &&
                    !isLegacySwipeDown() &&
                    (suppressionMask and FLAG_SUPPRESS_SWIPE_DOWN) == 0 &&
                    isSlotBoundTo(GestureSlot.SWIPE_DOWN, NovaAction.EXPAND_STATUS_BAR)
                ) {
                    val dy = ev.y - singleDownY
                    val dx = ev.x - singleDownX
                    if (dy > 0f &&
                        abs(dy) > abs(dx) * 1.25f &&
                        GestureMath.shouldTriggerIncrementalSwipeDown(
                            deltaY = dy,
                            isRelease = true,
                            immediateThresholdPx = swipeDownImmediatePx,
                            releaseThresholdPx = swipeDownReleasePx
                        )
                    ) {
                        incrementalDownFired = true
                        lockedForCurrentSequence = true
                        if (isSwipeDownHapticEnabled()) {
                            hostViewProvider()?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        }
                        onSlotTriggered(GestureSlot.SWIPE_DOWN)
                        return true
                    }
                }
            }
        }

        simpleGestureDetector.onTouchEvent(ev)
        if (ev.actionMasked == MotionEvent.ACTION_UP || ev.actionMasked == MotionEvent.ACTION_CANCEL) {
            twoFingerActive = false
            incrementalDownFired = false
        }
        return lockedForCurrentSequence
    }

    companion object {
        const val FLAG_SUPPRESS_SWIPE_UP = 1
        const val FLAG_SUPPRESS_SWIPE_DOWN = 2
    }
}
