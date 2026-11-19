package com.example.launcherai.ui

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Application-level System Bar (Status Bar & Navigation Bar) manager (Phase 9.3R).
 * Configures edge-to-edge transparent system bars, light/dark status bar icon appearances,
 * contrast enforcement disabling, transient navigation bar behaviors, and ViewCompat WindowInsets padding listeners.
 */
object SystemBarHelper {

    /**
     * Applies system bar configuration for light or dark themes across activities.
     * @param activity The target Activity.
     * @param isLightTheme True for light background (dark status/nav icons), false for dark background (light status/nav icons).
     * @param transparentBars True if status bar and navigation bar should be fully transparent (e.g. for wallpaper desktop).
     * @param hideNavigationBar True to hide system navigation bars with swipe-to-show transient behavior.
     */
    fun applySystemBars(
        activity: Activity,
        isLightTheme: Boolean,
        transparentBars: Boolean = false,
        hideNavigationBar: Boolean = false
    ) {
        val window = activity.window ?: return

        WindowCompat.setDecorFitsSystemWindows(window, !transparentBars)

        if (transparentBars) {
            @Suppress("DEPRECATION")
            window.statusBarColor = Color.TRANSPARENT
            @Suppress("DEPRECATION")
            window.navigationBarColor = Color.TRANSPARENT
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            @Suppress("DEPRECATION")
            window.isStatusBarContrastEnforced = false
            @Suppress("DEPRECATION")
            window.isNavigationBarContrastEnforced = false
        }

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = isLightTheme
        controller.isAppearanceLightNavigationBars = isLightTheme

        if (hideNavigationBar) {
            controller.hide(WindowInsetsCompat.Type.navigationBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    /**
     * Applies Status Bar + Display Cutout top padding dynamically to a header view.
     */
    fun applyStatusBarInsets(insetTargetView: View) {
        val initialTopPadding = insetTargetView.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(insetTargetView) { view, insets ->
            val statusBarInsets = insets.getInsets(
                WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(
                view.paddingLeft,
                initialTopPadding + statusBarInsets.top,
                view.paddingRight,
                view.paddingBottom
            )
            insets
        }
        ViewCompat.requestApplyInsets(insetTargetView)
    }

    /**
     * Applies Navigation Bar bottom padding dynamically to a dock/bottom nav view.
     */
    fun applyNavigationBarInsets(insetTargetView: View) {
        val initialBottomPadding = insetTargetView.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(insetTargetView) { view, insets ->
            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                initialBottomPadding + navBarInsets.bottom
            )
            insets
        }
        ViewCompat.requestApplyInsets(insetTargetView)
    }

    /**
     * Applies status bar top padding to [topInsetView] and navigation bar bottom padding to [bottomInsetView].
     */
    fun applyEdgeToEdgeInsets(
        insetsRootView: View,
        topInsetView: View? = null,
        bottomInsetView: View? = null
    ) {
        val initialTopViewPadding = topInsetView?.paddingTop ?: 0
        val initialBottomViewPadding = bottomInsetView?.paddingBottom ?: 0

        ViewCompat.setOnApplyWindowInsetsListener(insetsRootView) { _, insets ->
            val statusBarInsets = insets.getInsets(
                WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            topInsetView?.let { topView ->
                topView.setPadding(
                    topView.paddingLeft,
                    initialTopViewPadding + statusBarInsets.top,
                    topView.paddingRight,
                    topView.paddingBottom
                )
            }

            bottomInsetView?.let { bottomView ->
                bottomView.setPadding(
                    bottomView.paddingLeft,
                    bottomView.paddingTop,
                    bottomView.paddingRight,
                    initialBottomViewPadding + navBarInsets.bottom
                )
            }

            insets
        }
        ViewCompat.requestApplyInsets(insetsRootView)
    }
}
