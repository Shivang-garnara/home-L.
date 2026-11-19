package com.example.launcherai.ui

import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.launcherai.AppInfo

/**
 * FastScroller helper providing alphabetical section indexing and fast scroll bubble
 * indicator synchronization for App Drawer RecyclerView.
 */
class FastScroller(
    private val recyclerView: RecyclerView,
    private val bubbleView: TextView
) {

    private val hideHandler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable {
        bubbleView.animate().alpha(0f).setDuration(200).withEndAction {
            bubbleView.visibility = View.GONE
        }.start()
    }

    private var currentApps: List<AppInfo> = emptyList()

    init {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy != 0 && currentApps.isNotEmpty()) {
                    showScrollSectionBubble(rv)
                }
            }
        })
    }

    fun updateApps(apps: List<AppInfo>) {
        this.currentApps = apps
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
            hideHandler.postDelayed(hideRunnable, 1200)
        }
    }
}
