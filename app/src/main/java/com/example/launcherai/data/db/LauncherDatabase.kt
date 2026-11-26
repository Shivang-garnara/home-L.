package com.example.launcherai.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        WorkspaceItemEntity::class,
        WorkspacePageEntity::class,
        FolderEntity::class,
        ChatMessageEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class LauncherDatabase : RoomDatabase() {

    abstract fun workspaceDao(): WorkspaceDao
    abstract fun chatDao(): ChatDao

    companion object {
        private const val DATABASE_NAME = "launcher_ai.db"

        @Volatile
        private var INSTANCE: LauncherDatabase? = null

        /**
         * Migration from Database v1 -> v2 adding appWidgetId column for native widget hosting.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workspace_items ADD COLUMN appWidgetId INTEGER NOT NULL DEFAULT -1")
            }
        }

        /**
         * Migration from Database v2 -> v3 adding chat_messages table for conversation history persistence.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS chat_messages (
                        id TEXT NOT NULL PRIMARY KEY,
                        text TEXT NOT NULL,
                        isUser INTEGER NOT NULL,
                        timestamp TEXT NOT NULL,
                        reasoning TEXT,
                        toolCallName TEXT,
                        toolCallResult TEXT,
                        requiresConfirmation INTEGER NOT NULL DEFAULT 0,
                        isConfirmed INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * Migration from Database v3 -> v4 adding per-icon swipe-up and swipe-down gesture columns.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workspace_items ADD COLUMN swipeUpAction TEXT")
                db.execSQL("ALTER TABLE workspace_items ADD COLUMN swipeDownAction TEXT")
            }
        }

        fun getInstance(context: Context): LauncherDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LauncherDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
