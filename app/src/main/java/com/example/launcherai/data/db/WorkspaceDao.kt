package com.example.launcherai.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceDao {

    // --- Workspace Items ---

    @Query("SELECT * FROM workspace_items ORDER BY screenId ASC, cellY ASC, cellX ASC, rank ASC")
    fun getAllWorkspaceItemsFlow(): Flow<List<WorkspaceItemEntity>>

    @Query("SELECT * FROM workspace_items ORDER BY screenId ASC, cellY ASC, cellX ASC, rank ASC")
    suspend fun getAllWorkspaceItemsSync(): List<WorkspaceItemEntity>

    @Query("SELECT * FROM workspace_items WHERE container = :container ORDER BY cellY ASC, cellX ASC, rank ASC")
    fun getWorkspaceItemsByContainerFlow(container: Int): Flow<List<WorkspaceItemEntity>>

    @Query("SELECT * FROM workspace_items WHERE container = :container ORDER BY cellY ASC, cellX ASC, rank ASC")
    suspend fun getWorkspaceItemsByContainerSync(container: Int): List<WorkspaceItemEntity>

    @Query("SELECT * FROM workspace_items WHERE screenId = :screenId ORDER BY cellY ASC, cellX ASC, rank ASC")
    fun getWorkspaceItemsByScreenFlow(screenId: Int): Flow<List<WorkspaceItemEntity>>

    @Query("SELECT * FROM workspace_items WHERE id = :id LIMIT 1")
    suspend fun getWorkspaceItemById(id: Long): WorkspaceItemEntity?

    @Query("SELECT COUNT(*) FROM workspace_items")
    suspend fun getItemCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkspaceItem(item: WorkspaceItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkspaceItems(items: List<WorkspaceItemEntity>): List<Long>

    @Update
    suspend fun updateWorkspaceItem(item: WorkspaceItemEntity)

    @Delete
    suspend fun deleteWorkspaceItem(item: WorkspaceItemEntity)

    @Query("SELECT * FROM workspace_items WHERE packageName = :packageName")
    suspend fun getItemsByPackageName(packageName: String): List<WorkspaceItemEntity>

    @Query("DELETE FROM workspace_items WHERE id = :id")
    suspend fun deleteWorkspaceItemById(id: Long)

    @Query("DELETE FROM workspace_items WHERE packageName = :packageName")
    suspend fun deleteWorkspaceItemsByPackageName(packageName: String): Int

    // --- Workspace Pages ---

    @Query("SELECT * FROM workspace_pages ORDER BY pageOrder ASC")
    fun getAllPagesFlow(): Flow<List<WorkspacePageEntity>>

    @Query("SELECT * FROM workspace_pages ORDER BY pageOrder ASC")
    suspend fun getAllPagesSync(): List<WorkspacePageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: WorkspacePageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPages(pages: List<WorkspacePageEntity>)

    @Query("UPDATE workspace_pages SET pageOrder = :pageOrder WHERE screenId = :screenId")
    suspend fun updatePageOrder(screenId: Int, pageOrder: Int)

    @Query("SELECT MAX(screenId) FROM workspace_pages")
    suspend fun getMaxScreenId(): Int?

    @Query("SELECT * FROM workspace_items WHERE screenId = :screenId")
    suspend fun getWorkspaceItemsByScreenSync(screenId: Int): List<WorkspaceItemEntity>

    @Query("DELETE FROM workspace_pages WHERE screenId = :screenId")
    suspend fun deletePage(screenId: Int)

    // --- Folders ---

    @Query("SELECT * FROM folders ORDER BY folderId ASC")
    fun getAllFoldersFlow(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders ORDER BY folderId ASC")
    suspend fun getAllFoldersSync(): List<FolderEntity>

    @Query("SELECT * FROM folders WHERE folderId = :folderId LIMIT 1")
    suspend fun getFolderById(folderId: Long): FolderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity): Long

    @Update
    suspend fun updateFolder(folder: FolderEntity)

    @Query("DELETE FROM folders WHERE folderId = :folderId")
    suspend fun deleteFolder(folderId: Long)
}
