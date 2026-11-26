package com.example.launcherai.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workspace_items",
    indices = [
        Index(value = ["packageName"]),
        Index(value = ["screenId"]),
        Index(value = ["container"])
    ]
)
data class WorkspaceItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val itemType: String,
    val packageName: String? = null,
    val className: String? = null,
    val container: Int = CONTAINER_DESKTOP,
    val screenId: Int = 0,
    val cellX: Int = 0,
    val cellY: Int = 0,
    val spanX: Int = 1,
    val spanY: Int = 1,
    val rank: Int = 0,
    val title: String? = null,
    val appWidgetId: Int = -1,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val CONTAINER_DESKTOP = -100
        const val CONTAINER_DOCK = -101
        const val CONTAINER_DRAWER = -102

        const val ITEM_TYPE_APP = "APP"
        const val ITEM_TYPE_FOLDER = "FOLDER"
        const val ITEM_TYPE_WIDGET = "WIDGET"
    }
}
