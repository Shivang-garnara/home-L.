package com.example.launcherai.ui

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.launcherai.data.model.AppInfo

/**
 * FastScroller coordinator providing alphabetical section indexing, A–Z rail navigation,
 * and popup letter bubble positioning matching Nova Launcher's RecyclerViewFastScroller.
 */
class FastScroller(
    private val recyclerView: RecyclerView,
    private val bubbleView: TextView,
    private val railView: FastScrollRailView? = null
) {

    var isEnabled: Boolean = true
        set(value) {
            field = value
            railView?.visibility = if (value) View.VISIBLE else View.GONE
            if (!value) {
                bubbleView.visibility = View.GONE
            }
        }

    private val hideHandler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable {
        bubbleView.animate().alpha(0f).setDuration(180).withEndAction {
            bubbleView.visibility = View.GONE
        }.start()
    }

    private var currentApps: List<AppInfo> = emptyList()

    init {
        // Setup Rail listener
        railView?.onSectionChangeListener = { letter, touchY ->
            if (isEnabled && currentApps.isNotEmpty()) {
                jumpToSection(letter, touchY)
            }
        }

        railView?.onScrollEndListener = {
            hideHandler.removeCallbacks(hideRunnable)
            hideHandler.postDelayed(hideRunnable, 400)
        }

        // Setup RecyclerView scroll listener
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (!isEnabled || currentApps.isEmpty()) return

                val offset = rv.computeVerticalScrollOffset()
                val extent = rv.computeVerticalScrollExtent()
                val range = rv.computeVerticalScrollRange()
                val maxScroll = (range - extent).coerceAtLeast(1)
                val ratio = (offset.toFloat() / maxScroll).coerceIn(0f, 1f)

                railView?.setScrollProgress(ratio)

                if (dy != 0 && bubbleView.visibility == View.VISIBLE) {
                    showScrollSectionBubble(rv)
                }
            }
        })
    }

    fun updateApps(apps: List<AppInfo>) {
        this.currentApps = apps
    }

    fun setAccentColor(color: Int) {
        railView?.accentColor = color
        val drawable = bubbleView.background?.mutate()
        drawable?.setTint(color)
    }

    private fun jumpToSection(letter: String, touchY: Float) {
        val targetIndex = if (letter == "#") {
            0
        } else {
            val targetChar = letter.firstOrNull() ?: return
            val firstNonFolder = currentApps.indexOfFirst { !it.isFolder }
            val hasFoldersGroupedAtTop = firstNonFolder > 0 && currentApps.take(firstNonFolder).all { it.isFolder }
            currentApps.indexOfFirst { app ->
                if (hasFoldersGroupedAtTop && app.isFolder) {
                    false
                } else {
                    val firstChar = app.label.firstOrNull()?.uppercaseChar() ?: '#'
                    if (targetChar.isLetter()) {
                        firstChar >= targetChar
                    } else {
                        true
                    }
                }
            }.let { idx -> if (idx >= 0) idx else 0 }
        }

        if (targetIndex >= 0 && targetIndex < currentApps.size) {
            val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
            layoutManager?.scrollToPositionWithOffset(targetIndex, 0)

            bubbleView.text = letter
            val density = bubbleView.resources.displayMetrics.density
            val bubbleHeight = if (bubbleView.height > 0) bubbleView.height else (56 * density).toInt()
            val rvHeight = recyclerView.height
            val targetY = (touchY - (bubbleHeight / 2f)).coerceIn(16f * density, (rvHeight - bubbleHeight - 16 * density).coerceAtLeast(0f))

            bubbleView.translationY = targetY
            hideHandler.removeCallbacks(hideRunnable)

            if (bubbleView.visibility != View.VISIBLE) {
                bubbleView.visibility = View.VISIBLE
                bubbleView.alpha = 1f
            }

            val prefs = recyclerView.context.getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
            if (prefs.getBoolean("key_drawer_swipe_haptic", true)) {
                railView?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
        }
    }

    private fun showScrollSectionBubble(rv: RecyclerView) {
        val layoutManager = rv.layoutManager as? LinearLayoutManager ?: return
        val firstVisiblePos = layoutManager.findFirstVisibleItemPosition()
        if (firstVisiblePos in currentApps.indices) {
            val app = currentApps[firstVisiblePos]
            val firstChar = app.label.firstOrNull()?.uppercaseChar() ?: '#'
            val letter = if (firstChar.isLetter()) firstChar.toString() else "#"

            bubbleView.text = letter
            if (bubbleView.visibility != View.VISIBLE) {
                bubbleView.visibility = View.VISIBLE
                bubbleView.alpha = 1f
            }

            hideHandler.removeCallbacks(hideRunnable)
            hideHandler.postDelayed(hideRunnable, 1000)
        }
    }
}
