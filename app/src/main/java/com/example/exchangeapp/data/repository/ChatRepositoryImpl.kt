package com.example.exchangeapp.data.repository

import com.example.exchangeapp.data.local.dao.ChatDao
import com.example.exchangeapp.data.local.entity.ChatMessageEntity
import com.example.exchangeapp.domain.model.ChatMessage
import com.example.exchangeapp.domain.model.MessageType
import com.example.exchangeapp.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 聊天仓库实现类。
 *
 * 基于 Room 的 [ChatDao] 完成聊天消息的本地持久化，并负责 [ChatMessageEntity]
 * 与领域模型 [ChatMessage] 之间的相互转换。其中：
 * - 会话标识(conversationId)由两个用户 id 排序后拼接生成，保证同一对用户得到一致的会话 id；
 * - 消息类型(messageType)以枚举名称的字符串形式持久化。
 *
 * Requirements: 9.3（会话消息读取）、9.4（消息发送）、9.5（已读标记）、9.7（会话实时观察）。
 */
@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val chatDao: ChatDao
) : ChatRepository {

    override suspend fun getConversation(userId1: String, userId2: String): List<ChatMessage> {
        val conversationId = generateConversationId(userId1, userId2)
        return chatDao.getMessagesByConversationId(conversationId).map { it.toModel() }
    }

    override suspend fun sendMessage(message: ChatMessage): Result<Unit> {
        return try {
            chatDao.insertMessage(message.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markAsRead(conversationId: String) {
        chatDao.markMessagesAsRead(conversationId)
    }

    override fun observeConversation(conversationId: String): Flow<List<ChatMessage>> {
        return chatDao.observeMessages(conversationId)
            .map { entities -> entities.map { it.toModel() } }
    }

    override suspend fun getConversationsForUser(userId: String): List<ChatMessage> {
        return chatDao.getLatestMessagesForUser(userId).map { it.toModel() }
    }

    override suspend fun getUnreadCount(conversationId: String, userId: String): Int {
        return chatDao.getUnreadCountForConversation(conversationId, userId)
    }

    /**
     * 根据两个用户 id 生成会话标识。
     * 对 id 排序后拼接，确保 (a, b) 与 (b, a) 得到相同的会话标识。
     */
    private fun generateConversationId(userId1: String, userId2: String): String {
        val sorted = listOf(userId1, userId2).sorted()
        return "${sorted[0]}_${sorted[1]}"
    }

    // region 转换逻辑（Entity <-> Model）

    /**
     * 将领域模型 [ChatMessage] 转换为持久化实体 [ChatMessageEntity]。
     */
    private fun ChatMessage.toEntity(): ChatMessageEntity {
        return ChatMessageEntity(
            id = id,
            conversationId = conversationId,
            senderId = senderId,
            receiverId = receiverId,
            content = content,
            messageType = messageType.name,
            imageUrl = imageUrl,
            timestamp = timestamp,
            isRead = isRead
        )
    }

    /**
     * 将持久化实体 [ChatMessageEntity] 转换为领域模型 [ChatMessage]。
     */
    private fun ChatMessageEntity.toModel(): ChatMessage {
        return ChatMessage(
            id = id,
            conversationId = conversationId,
            senderId = senderId,
            receiverId = receiverId,
            content = content,
            messageType = decodeMessageType(messageType),
            imageUrl = imageUrl,
            timestamp = timestamp,
            isRead = isRead
        )
    }

    /**
     * 将字符串解析为 [MessageType]；无法识别时回退为 [MessageType.TEXT]。
     */
    private fun decodeMessageType(value: String): MessageType {
        return try {
            MessageType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            MessageType.TEXT
        }
    }

    // endregion
}
