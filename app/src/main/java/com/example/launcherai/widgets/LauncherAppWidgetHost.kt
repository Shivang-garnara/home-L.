package com.example.launcherai.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context

/**
 * Custom AppWidgetHost for LauncherAI using stable host ID 1024.
 * Manages widget lifecycle, view creation, and widget listening state.
 */
class LauncherAppWidgetHost(
    context: Context,
    hostId: Int = APPWIDGET_HOST_ID
) : AppWidgetHost(context.applicationContext, hostId) {

    override fun onCreateView(
        context: Context,
        appWidgetId: Int,
        appWidget: AppWidgetProviderInfo?
    ): AppWidgetHostView {
        return LauncherAppWidgetHostView(context)
    }

    companion object {
        const val APPWIDGET_HOST_ID = 1024
    }
}
