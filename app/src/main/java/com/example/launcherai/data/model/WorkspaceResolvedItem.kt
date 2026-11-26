package com.example.launcherai.data.model

import com.example.launcherai.data.db.WorkspaceItemEntity

data class WorkspaceResolvedItem(
    val entity: WorkspaceItemEntity,
    val appInfo: AppInfo
)