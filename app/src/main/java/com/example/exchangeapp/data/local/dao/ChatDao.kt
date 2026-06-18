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

    /**
     * 获取与指定用户相关的每个会话的最新一条消息。
     *
     * 通过子查询取出每个 conversationId 下的最大时间戳，再筛选属于该用户
     * (作为发送者或接收者) 的消息，按时间倒序返回，用于会话列表展示。
     */
    @Query("SELECT * FROM chat_messages WHERE (senderId = :userId OR receiverId = :userId) AND timestamp IN (SELECT MAX(timestamp) FROM chat_messages WHERE senderId = :userId OR receiverId = :userId GROUP BY conversationId) ORDER BY timestamp DESC")
    suspend fun getLatestMessagesForUser(userId: String): List<ChatMessageEntity>

    /**
     * 统计指定会话中该用户尚未读取的消息数量。
     */
    @Query("SELECT COUNT(*) FROM chat_messages WHERE conversationId = :conversationId AND receiverId = :userId AND isRead = 0")
    suspend fun getUnreadCountForConversation(conversationId: String, userId: String): Int
}
