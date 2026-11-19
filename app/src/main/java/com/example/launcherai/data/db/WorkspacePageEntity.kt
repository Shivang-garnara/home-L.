package com.example.launcherai.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workspace_pages")
data class WorkspacePageEntity(
    @PrimaryKey
    val screenId: Int,
    val pageOrder: Int
)
