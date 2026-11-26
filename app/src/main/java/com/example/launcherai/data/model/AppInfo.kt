package com.example.launcherai.data.model

import android.graphics.drawable.Drawable

data class AppInfo(
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: Drawable? = null,
    val isSystem: Boolean = false,
    val category: String = CATEGORY_OTHER,
    val isFolder: Boolean = false,
    val folderId: Long? = null,
    val folderChildren: List<AppInfo> = emptyList(),
    val keepAppsInMainDrawer: Boolean = false,
    val smartCategoryId: String? = null,
    val autoAddNewApps: Boolean = false
) {
    companion object {
        const val CATEGORY_ALL = "All"
        const val CATEGORY_ENTERTAINMENT = "Entertainment"
        const val CATEGORY_FINANCE = "Finance"
        const val CATEGORY_FOOD = "Food"
        const val CATEGORY_GAMES = "Games"
        const val CATEGORY_SHOPPING = "Shopping"
        const val CATEGORY_SOCIAL = "Social"
        const val CATEGORY_TRAVEL = "Travel"
        const val CATEGORY_COMMUNICATION = "Communication"
        const val CATEGORY_PHOTOGRAPHY = "Photography"
        const val CATEGORY_PRODUCTIVITY = "Productivity"
        const val CATEGORY_TOOLS = "Tools"
        const val CATEGORY_OTHER = "Other"
    }
}
