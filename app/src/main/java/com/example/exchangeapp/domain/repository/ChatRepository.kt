package com.example.exchangeapp.domain.repository

import com.example.exchangeapp.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

/**
 * 聊天仓库接口。
 *
 * 负责聊天消息的读取、发送、已读标记以及会话的实时观察。
 *
 * Requirements: 8.2（即时通讯与消息管理）、10.1（数据本地持久化）。
 */
interface ChatRepository {
    /**
     * 获取两个用户之间的会话消息列表（按时间升序）。
     */
    suspend fun getConversation(userId1: String, userId2: String): List<ChatMessage>

    /**
     * 发送一条消息，成功返回 [Result.success]，失败返回 [Result.failure]。
     */
    suspend fun sendMessage(message: ChatMessage): Result<Unit>

    /**
     * 将指定会话中的消息全部标记为已读。
     */
    suspend fun markAsRead(conversationId: String)

    /**
     * 观察指定会话的消息流，会话内容变化时实时推送。
     */
    fun observeConversation(conversationId: String): Flow<List<ChatMessage>>

    /**
     * 获取指定用户的会话列表，每个会话返回其最新的一条消息（按时间倒序）。
     */
    suspend fun getConversationsForUser(userId: String): List<ChatMessage>

    /**
     * 获取指定会话中该用户尚未读取的消息数量。
     */
    suspend fun getUnreadCount(conversationId: String, userId: String): Int
}
