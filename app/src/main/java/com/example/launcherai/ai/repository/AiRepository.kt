package com.example.launcherai.ai.repository

import android.content.Context
import com.example.launcherai.data.db.ChatDao
import com.example.launcherai.data.db.ChatMessageEntity
import com.example.launcherai.data.db.LauncherDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class AiRepository(
    private val context: Context,
    private val chatDao: ChatDao = LauncherDatabase.getInstance(context).chatDao()
) {

    val conversationHistoryFlow: Flow<List<ChatMessageEntity>> = chatDao.getAllMessagesFlow()

    suspend fun saveMessage(message: ChatMessageEntity) = withContext(Dispatchers.IO) {
        chatDao.insertMessage(message)
    }

    suspend fun updateMessage(message: ChatMessageEntity) = withContext(Dispatchers.IO) {
        chatDao.updateMessage(message)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        chatDao.clearAllMessages()
    }
}
