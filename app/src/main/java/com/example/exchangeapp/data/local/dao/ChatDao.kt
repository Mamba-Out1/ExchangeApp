package com.example.exchangeapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.exchangeapp.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    suspend fun getMessagesByConversationId(conversationId: String): List<ChatMessageEntity>
    
    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun observeMessages(conversationId: String): Flow<List<ChatMessageEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)
    
    @Query("UPDATE chat_messages SET isRead = 1 WHERE conversationId = :conversationId")
    suspend fun markMessagesAsRead(conversationId: String)
    
    @Query("SELECT COUNT(*) FROM chat_messages WHERE receiverId = :userId AND isRead = 0")
    fun getUnreadCount(userId: String): Flow<Int>
}
