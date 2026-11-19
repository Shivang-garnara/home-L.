package com.example.launcherai.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "folders",
    indices = [
        Index(value = ["screenId"])
    ]
)
data class FolderEntity(
    @PrimaryKey(autoGenerate = true)
    val folderId: Long = 0,
    val title: String,
    val screenId: Int = 0,
    val cellX: Int = 0,
    val cellY: Int = 0
)
