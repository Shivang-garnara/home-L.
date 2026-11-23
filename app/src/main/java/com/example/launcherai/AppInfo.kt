package com.example.launcherai

import android.graphics.drawable.Drawable

data class AppInfo(
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: Drawable? = null,
    val isSystem: Boolean = false,
    val category: String = CATEGORY_OTHER,
    val isFolder: Boolean = false,
    val folderId: Long? = null
) {
    companion object {
        const val CATEGORY_ALL = "All"
        const val CATEGORY_COMMUNICATION = "Communication"
        const val CATEGORY_ENTERTAINMENT = "Entertainment"
        const val CATEGORY_GAMES = "Games"
        const val CATEGORY_PHOTOGRAPHY = "Photography"
        const val CATEGORY_PRODUCTIVITY = "Productivity"
        const val CATEGORY_TOOLS = "Tools"
        const val CATEGORY_OTHER = "Other"
    }
}
